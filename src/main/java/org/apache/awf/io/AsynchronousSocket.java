/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.awf.io;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.apache.awf.util.Closeables;
import org.apache.awf.util.KnuthMorrisPrattAlgorithm;
import org.apache.awf.util.NopAsyncResult;
import org.apache.awf.web.AsyncCallback;
import org.apache.awf.web.AsyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousSocket implements IOHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousSocket.class);

    private final IOLoop ioLoop;

    private static final int DEFAULT_BYTEBUFFER_SIZE = 1024;
    private static final int DEFAULT_INITIAL_READ_BYTEBUFFER_SIZE = 1024;
    private static final int DEFAULT_INITIAL_WRITE_BYTEBUFFER_SIZE = 1024;

    private final AsyncResult<byte[]> nopAsyncByteArrayResult = NopAsyncResult.of(byte[].class).nopAsyncResult;
    private final AsyncResult<Boolean> nopAsyncBooleanResult = NopAsyncResult.of(Boolean.class).nopAsyncResult;

    private final SocketChannel channel;
    private int interestOps;

    private byte[] readDelimiter = "".getBytes();
    private int readBytes = Integer.MAX_VALUE;

    private AsyncResult<Boolean> connectCallback = nopAsyncBooleanResult;
    private AsyncCallback closeCallback = AsyncCallback.nopCb;
    private AsyncResult<byte[]> readCallback = nopAsyncByteArrayResult;
    private AsyncCallback writeCallback = AsyncCallback.nopCb;

    private final DynamicByteBuffer readBuffer = DynamicByteBuffer.allocate(DEFAULT_INITIAL_READ_BYTEBUFFER_SIZE);
    private final DynamicByteBuffer writeBuffer = DynamicByteBuffer.allocate(DEFAULT_INITIAL_WRITE_BYTEBUFFER_SIZE);

    private boolean reachedEOF = false;
    
    private long connectedTime = -1;

    /**
     * Creates a new {@code AsynchronousSocket} that will delegate its io
     * operations to the given {@link SelectableChannel}.
     * <p>
     * Support for three non-blocking asynchronous methods that take callbacks:
     * <p>
     * {@link #readUntil(byte[], AsyncResult)}
     * <p>
     * {@link #readBytes(int, AsyncResult)} and
     * <p>
     * {@link #write(byte[], AsyncCallback)}
     * <p>
     * The {@link SelectableChannel} should be the result of either
     * {@link SocketChannel#open()} (client operations, connected or
     * unconnected) or {@link ServerSocketChannel#accept()} (server operations).
     * <p>
     * The given {@code SelectableChannel} will be configured to be in
     * non-blocking mode, even if it is non-blocking already.
     * 
     * <p>
     * Below is an example of how a simple server could be implemented.
     * 
     * <pre>
     *   final ServerSocketChannel server = ServerSocketChannel.open();
     *   server.socket().bind(new InetSocketAddress(9090));
     * 	
     *   AcceptUtil.accept(server, new AsyncCallback() { public void onCallback() { onAccept(server);} });
     *   IOLoop.INSTANCE.start();
     * 
     *   private static void onAccept(ServerSocketChannel channel) {
     *       SocketChannel client = channel.accept();
     *       AsynchronousSocket socket = new AsynchronousSocket(client);
     *       // use socket
     *   }
     * </pre>
     */
    public AsynchronousSocket(SocketChannel channel) {
        this(IOLoop.INSTANCE, channel);
    }

    public AsynchronousSocket(IOLoop ioLoop, SocketChannel channel) {
        this.ioLoop = ioLoop;
        this.channel = channel;
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            logger.error("Could not configure SocketChannel to be non-blocking");
        }
        if (channel.isConnected()) {
            interestOps |= SelectionKey.OP_READ;
        }
        ioLoop.addHandler(channel, this, interestOps, null);
    }

    /**
     * Connects to the given host port tuple and invokes the given callback when
     * a successful connection is established.
     * <p>
     * You can both read and write on the {@code AsynchronousSocket} before it
     * is connected (in which case the data will be written/read as soon as the
     * connection is ready).
     */
    public void connect(String host, int port, AsyncResult<Boolean> ccb) {
        ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_CONNECT);
        connectCallback = ccb;
        try {
            channel.connect(new InetSocketAddress(host, port));
        } catch (IOException e) {
            logger.error("Failed to connect to: {}, message: {} ", host, e.getMessage());
            invokeConnectFailureCallback(e);
        } catch (UnresolvedAddressException e) {
            logger.warn("Unresolvable host: {}", host);
            invokeConnectFailureCallback(e);
        }
    }

    /**
     * Close the socket.
     */
    public void close() {
        Closeables.closeQuietly(ioLoop, channel);
        invokeCloseCallback();
    }

    /**
     * The given callback will invoked when the underlaying {@code
     * SelectableChannel} is closed.
     */
    public void setCloseCallback(AsyncCallback ccb) {
        closeCallback = ccb;
    }

    /**
     * Should only be invoked by the IOLoop
     */
    @Override
    public void handleAccept(SelectionKey key) throws IOException {
        logger.debug("handle accept...");
    }

    /**
     * Should only be invoked by the IOLoop
     */
    @Override
    public void handleConnect(SelectionKey key) throws IOException {
        logger.debug("handle connect...");
        if (channel.isConnectionPending()) {
            try {
                channel.finishConnect();
                connectedTime = System.currentTimeMillis();
                invokeConnectSuccessfulCallback();
                interestOps &= ~SelectionKey.OP_CONNECT;
                ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_READ);
            } catch (ConnectException e) {
                logger.warn("Connect failed: {}", e.getMessage());
                invokeConnectFailureCallback(e);
            }
        }
    }

    /**
     * Should only be invoked by the IOLoop
     */
    @Override
    public void handleRead(SelectionKey key) throws IOException {
        logger.debug("handle read...");
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BYTEBUFFER_SIZE);
        // TODO RS 110723 reuse byte buffers
        int read = 0;
        try {
            read = channel.read(buffer);
        } catch (IOException e) {
            logger.error("IOException during read: {}", e.getMessage());
            invokeCloseCallback();
            Closeables.closeQuietly(ioLoop, channel);
            return;
        }

        if (read == -1) { // EOF
            reachedEOF = true;
            ioLoop.updateHandler(channel, interestOps &= ~SelectionKey.OP_READ);
            if (writeBuffer.position() == 0) {
                invokeCloseCallback();
            }

            return;
        }
        buffer.flip();
        readBuffer.put(buffer);
        logger.debug("readBuffer size: {}", readBuffer.position());
        checkReadState();
    }

    /**
     * Should only be invoked by the IOLoop
     */
    @Override
    public void handleWrite(SelectionKey key) {
        logger.debug("handle write...");
        doWrite();
    }

    /**
     * Reads from the underlaying SelectableChannel until delimiter is reached.
     * When it its, the given AsyncResult will be invoked.
     */
    public void readUntil(byte[] delimiter, AsyncResult<byte[]> rcb) {
        logger.debug("readUntil delimiter: {}", new String(delimiter));
        readDelimiter = delimiter;
        readCallback = rcb;
        checkReadState();
    }

    /**
     * Reads from the underlaying SelectableChannel until n bytes are read. When
     * it its, the given AsyncResult will be invoked.
     */
    public void readBytes(int n, AsyncResult<byte[]> rcb) {
        logger.debug("readBytes #bytes: {}", n);
        readBytes = n;
        readCallback = rcb;
        checkReadState();
    }

    /**
     * If readBuffer contains readDelimiter, client read is finished => invoke
     * readCallback (onSuccess) Or if readBytes bytes are read, client read is
     * finished => invoke readCallback (onSuccess) Of if end-of-stream is
     * reached => invoke readCallback (onFailure)
     */
    private void checkReadState() {
        if (reachedEOF) {
            invokeReadFailureCallback(new EOFException("Reached end-of-stream"));
            return;
        }
        int index = KnuthMorrisPrattAlgorithm.indexOf(readBuffer.array(), 0, readBuffer.position(), readDelimiter);
        if (index != -1 && readDelimiter.length > 0) {
            byte[] result = getResult(index, readDelimiter.length);
            readDelimiter = "".getBytes();
            invokeReadSuccessfulCallback(result);
        } else if (readBuffer.position() >= readBytes) {
            byte[] result = getResult(readBytes, 0);
            readBytes = Integer.MAX_VALUE;
            invokeReadSuccessfulCallback(result);
        }
    }

    /**
     * Returns the resulting byte[] data that was requested by the client
     * through readUntil(..) or readBytes(..)
     * 
     * @param size Number of bytes to fetch and remove from the read buffer.
     * @param advance The number of bytes the read buffer's position should move
     *            forward after the data has been fetched. (To ignore the
     *            readDelimiter.)
     */
    private byte[] getResult(int size, int advance) {
        readBuffer.flip();
        byte[] result = new byte[size];
        readBuffer.get(result, 0, size);
        // ignore the delimiter (if it was a readUntil(..) call)
        readBuffer.position(readBuffer.position() + advance);
        // "delete" the result data (data after result is left intact and will
        // not be overwritten)
        readBuffer.compact();
        logger.debug("readBuffer size: {}", readBuffer.position());
        return result;
    }

    private void invokeReadSuccessfulCallback(byte[] result) {
        AsyncResult<byte[]> cb = readCallback;
        readCallback = nopAsyncByteArrayResult;
        cb.onSuccess(result);
    }

    private void invokeReadFailureCallback(Exception e) {
        AsyncResult<byte[]> cb = readCallback;
        readCallback = nopAsyncByteArrayResult;
        cb.onFailure(e);
    }

    private void invokeWriteCallback() {
        AsyncCallback cb = writeCallback;
        writeCallback = AsyncCallback.nopCb;
        cb.onCallback();
    }

    private void invokeCloseCallback() {
        AsyncCallback cb = closeCallback;
        closeCallback = AsyncCallback.nopCb;
        cb.onCallback();
    }

    private void invokeConnectSuccessfulCallback() {
        AsyncResult<Boolean> cb = connectCallback;
        connectCallback = nopAsyncBooleanResult;
        cb.onSuccess(true);
    }

    private void invokeConnectFailureCallback(Exception e) {
        AsyncResult<Boolean> cb = connectCallback;
        connectCallback = nopAsyncBooleanResult;
        cb.onFailure(e);
        ;
    }

    /**
     * Writes the given data to the underlaying SelectableChannel. When all data
     * is successfully transmitted, the given AsyncCallback will be invoked
     */
    public void write(byte[] data, AsyncCallback wcb) {
        logger.debug("write data: {}", new String(data));
        writeBuffer.put(data);
        logger.debug("writeBuffer size: {}", writeBuffer.position());
        writeCallback = wcb;
        doWrite();
    }

    /**
     * If we succeed to write everything in writeBuffer, client write is
     * finished => invoke writeCallback
     */
    private void doWrite() {
        int written = 0;
        try {
            if (channel.isConnected()) {
                writeBuffer.flip(); // prepare for write
                written = channel.write(writeBuffer.getByteBuffer());
                // make room for more data be "read" in
                writeBuffer.compact();
            }
        } catch (IOException e) {
            logger.error("IOException during write: {}", e.getMessage());
            invokeCloseCallback();
            Closeables.closeQuietly(ioLoop, channel);
            return;
        }
        logger.debug("wrote: {} bytes", written);
        logger.debug("writeBuffer size: {}", writeBuffer.position());
        if (writeBuffer.position() > 0) {
            ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_WRITE);
        } else {
            ioLoop.updateHandler(channel, interestOps &= ~SelectionKey.OP_WRITE);
            invokeWriteCallback();
        }
    }
    
    public boolean isConnected() {
    	return channel.isConnected();
    }

    /**
     * Get the absolute time when this socket connected
     * 
     * @return long System.currentTimeMillis() when the socket was connected or -1 if not connected
     */
    public long getConnectedTime() {
    	return connectedTime;
    }
}

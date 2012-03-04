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
package org.apache.awf.web.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.awf.io.IOHandler;
import org.apache.awf.io.IOLoop;
import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.util.Closeables;
import org.apache.awf.web.Application;
import org.apache.awf.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.awf.web.http.HttpServerDescriptor.KEEP_ALIVE_TIMEOUT;
import static org.apache.awf.web.http.HttpServerDescriptor.READ_BUFFER_SIZE;

public class HttpProtocol implements IOHandler {

    private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

    private final IOLoop ioLoop;
    private final Application application;

    private final HttpRequestParser parser;

    // a queue of half-baked (pending/unfinished) HTTP post request
    private final Map<SelectableChannel, HttpRequestImpl> partials = Maps.newHashMap();

    public HttpProtocol(Application app) {
        this(IOLoop.INSTANCE, app);
    }

    public HttpProtocol(IOLoop ioLoop, Application app) {
        this.ioLoop = ioLoop;
        application = app;
        parser = new HttpRequestParser();
    }

    @Override
    public void handleAccept(SelectionKey key) throws IOException {
        logger.debug("handle accept...");
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        if (clientChannel != null) {
            // could be null in a multithreaded environment because another
            // ioloop was "faster" to accept()
            clientChannel.configureBlocking(false);
            ioLoop.addHandler(clientChannel, this, SelectionKey.OP_READ, ByteBuffer.allocate(READ_BUFFER_SIZE));
        }
    }

    @Override
    public void handleConnect(SelectionKey key) throws IOException {
        logger.error("handle connect in HttpProcotol...");
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        logger.debug("handle read...");
        SocketChannel clientChannel = (SocketChannel) key.channel();
        HttpRequest request = getHttpRequest(key, clientChannel);

        // Request is null when End-of-Stream have been reached
        // No need to do more things right now
        if(request != null){        	
        	logger.debug("received request: \n"+request.toString()); 
            if (request.isKeepAlive()) {
                ioLoop.addKeepAliveTimeout(clientChannel, Timeout.newKeepAliveTimeout(ioLoop, clientChannel,
                        KEEP_ALIVE_TIMEOUT));
            }

            HttpResponse response = new HttpResponseImpl(this, key, request.isKeepAlive());
            response.setCreateETag(application.getConfiguration().shouldCreateETags());

            RequestHandler rh = application.getHandler(request);
            HttpRequestDispatcher.dispatch(rh, request, response);
            
            // Only close if not async. In that case its up to RH to close it
            if (!rh.isMethodAsynchronous(request.getMethod()) ) {
                response.finish();
            }
        }
    }

    @Override
    public void handleWrite(SelectionKey key) {
        logger.debug("handle write...");
        SocketChannel channel = (SocketChannel) key.channel();

        if (key.attachment() instanceof FileInputStream) {
            writeMappedByteBuffer(key, channel);
        } else if (key.attachment() instanceof DynamicByteBuffer) {
            writeDynamicByteBuffer(key, channel);
        }
        if (ioLoop.hasKeepAliveTimeout(channel)) {
            prolongKeepAliveTimeout(channel);
        }

    }

    private void writeMappedByteBuffer(SelectionKey key, SocketChannel channel) {
        FileInputStream fileInputStream = (FileInputStream) key.attachment();

        try {
            long bytesWritten = 0;
            FileChannel fileChannel = fileInputStream.getChannel();
            long sizeNeeded = fileChannel.size();
            bytesWritten = fileChannel.position();
            bytesWritten += fileChannel.transferTo(bytesWritten, sizeNeeded - bytesWritten, channel);

            if (bytesWritten < sizeNeeded){
                // Set channel Position to write rest of data from good starting offset
                fileChannel.position(bytesWritten);
            }else{
                // Only close channel when file is totally transferred to SocketChannel
                com.google.common.io.Closeables.closeQuietly(fileInputStream);
                closeOrRegisterForRead(key);
            }
        } catch (IOException e) {
            logger.error("Failed to send data to client: {}", e.getMessage());
            com.google.common.io.Closeables.closeQuietly(fileInputStream);
            Closeables.closeQuietly(channel);
        }
    }

    private void writeDynamicByteBuffer(SelectionKey key, SocketChannel channel) {
        DynamicByteBuffer dbb = (DynamicByteBuffer) key.attachment();
        logger.debug("pending data about to be written");
        ByteBuffer toSend = dbb.getByteBuffer();
        toSend.flip(); // prepare for write
        long bytesWritten = 0;
        try {
            bytesWritten = channel.write(toSend);
        } catch (IOException e) {
            logger.error("Failed to send data to client: {}", e.getMessage());
            Closeables.closeQuietly(channel);
        }
        logger.debug("sent {} bytes to wire", bytesWritten);
        if (!toSend.hasRemaining()) {
            logger.debug("sent all data in toSend buffer");
            closeOrRegisterForRead(key); // should probably only be done if the
            // HttpResponse is finished
        } else {
            toSend.compact(); // make room for more data be "read" in
        }
    }

    public void closeOrRegisterForRead(SelectionKey key) {
        if (key.isValid() && ioLoop.hasKeepAliveTimeout(key.channel())) {
            try {
                key.channel().register(key.selector(), SelectionKey.OP_READ, reuseAttachment(key));
                prolongKeepAliveTimeout(key.channel());
                logger.debug("keep-alive connection. registrating for read.");
            } catch (ClosedChannelException e) {
                logger.debug("ClosedChannelException while registrating key for read: {}", e.getMessage());
                Closeables.closeQuietly(ioLoop, key.channel());
            }
        } else {
            // http request should be finished and no 'keep-alive' => close
            // connection
            logger.debug("Closing finished (non keep-alive) http connection");
            Closeables.closeQuietly(ioLoop, key.channel());
        }
    }

    public void prolongKeepAliveTimeout(SelectableChannel channel) {
        ioLoop.addKeepAliveTimeout(channel, Timeout.newKeepAliveTimeout(ioLoop, channel, KEEP_ALIVE_TIMEOUT));
    }

    public IOLoop getIOLoop() {
        return ioLoop;
    }

    /**
     * Clears the buffer (prepares for reuse) attached to the given
     * SelectionKey.
     * 
     * @return A cleared (position=0, limit=capacity) ByteBuffer which is ready
     *         for new reads
     */
    private ByteBuffer reuseAttachment(SelectionKey key) {
        Object o = key.attachment();
        ByteBuffer attachment = null;
        if (o instanceof FileInputStream) {
            com.google.common.io.Closeables.closeQuietly(((FileInputStream)o));
            attachment = ByteBuffer.allocate(READ_BUFFER_SIZE);
        } else if (o instanceof DynamicByteBuffer) {
            attachment = ((DynamicByteBuffer) o).getByteBuffer();
        } else {
            attachment = (ByteBuffer) o;
        }

        if (attachment.capacity() < READ_BUFFER_SIZE) {
            attachment = ByteBuffer.allocate(READ_BUFFER_SIZE);
        }
        attachment.clear(); // prepare for reuse
        return attachment;
    }

    private HttpRequest getHttpRequest(SelectionKey key, SocketChannel clientChannel) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        int bytesRead = -1;
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            logger.warn("Could not read buffer: {}", e.getMessage());
            Closeables.closeQuietly(ioLoop, clientChannel);
        }
        buffer.flip();

        if (bytesRead < 0){
            // Client closed the socket on his side
            Closeables.closeQuietly(ioLoop, clientChannel);
            return null;
        }

        return doGetHttpRequest(key, clientChannel, buffer);
    }

    private HttpRequest doGetHttpRequest(SelectionKey key, SocketChannel clientChannel, ByteBuffer buffer) {
        // do we have any unfinished http post requests for this channel?
        HttpRequestImpl request = null;
        if (partials.containsKey(clientChannel)) {
            request = parser.parseRequestBuffer(buffer, partials.get(clientChannel));
            if (request.isFinished()) {
                // received the entire payload/body
                partials.remove(clientChannel);
            }
        } else {
            request = parser.parseRequestBuffer(buffer);
            if (!request.isFinished()) {
                partials.put(key.channel(), request);
            }
        }


        // set extra request info
        request.setRemoteHost(clientChannel.socket().getInetAddress());
        request.setRemotePort(clientChannel.socket().getPort());
        request.setServerHost(clientChannel.socket().getLocalAddress());
        request.setServerPort(clientChannel.socket().getLocalPort());
        
        return (request.isFinished() || request.expectContinue() ? request : null);
    }

    @Override
    public String toString() {
        return "HttpProtocol";
    }
}

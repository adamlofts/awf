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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.awf.io.AsynchronousSocket;
import org.apache.awf.io.IOLoop;
import org.apache.awf.web.AsyncCallback;
import org.apache.awf.web.AsyncResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsynchronousSocketTest {

    public static final String HOST = "localhost";
    public static final int PORT = 6228;

    @Before
    public void setup() throws InterruptedException {
        // start the IOLoop from a new thread so we dont block this test.
        new Thread(new Runnable() {
            @Override
            public void run() {
                IOLoop.INSTANCE.start();
            }
        }).start();
        Thread.sleep(300); // hack to avoid SLF4J warning

        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {

            @Override
            public void run() {
                DataInputStream is = null;
                DataOutputStream os = null;
                try {
                    System.out.println("waiting for client...");
                    ServerSocket server = new ServerSocket(PORT);
                    latch.countDown();
                    Socket client = server.accept();
                    System.out.println("client connected..");
                    is = new DataInputStream(client.getInputStream());
                    os = new DataOutputStream(client.getOutputStream());

                    String recevied = is.readLine();
                    System.out.println("about to send: " + recevied);
                    os.writeBytes(recevied.toUpperCase());
                    System.out.println("sent data to client, shutdown server...");
                    server.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    closeQuietly(is, os);
                }
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws InterruptedException {
        IOLoop.INSTANCE.addCallback(new AsyncCallback() {
            @Override
            public void onCallback() {
                IOLoop.INSTANCE.stop();
            }
        });
        Thread.sleep(300); // give the IOLoop thread some time to gracefully
                           // shutdown
    }

    private void closeQuietly(InputStream is, OutputStream os) {
        try {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final AsynchronousSocket socket;
    private final CountDownLatch latch = new CountDownLatch(3); // 3
                                                                // op/callbacks
                                                                // (connect,
                                                                // write, read)

    public AsynchronousSocketTest() throws IOException {
        socket = new AsynchronousSocket(SocketChannel.open());
    }

    @Test
    public void connectWriteAndReadCallbackTest() throws InterruptedException, IOException {
        AsyncResult<Boolean> ccb = new AsyncResult<Boolean>() {
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(Boolean result) {
                onConnect();
            }
        };
        socket.connect(HOST, PORT, ccb);

        latch.await(20, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
        // TODO stop ioloop
    }

    private void onConnect() {
        latch.countDown();
        AsyncCallback wcb = new AsyncCallback() {
            @Override
            public void onCallback() {
                onWriteComplete();
            }
        };
        socket.write("roger|\r\n".getBytes(), wcb);
    }

    private void onWriteComplete() {
        latch.countDown();
        AsyncResult<byte[]> rcb = new AsyncResult<byte[]>() {
            @Override
            public void onFailure(Throwable caught) {
                assertTrue(false);
            }

            @Override
            public void onSuccess(byte[] result) {
                onReadComplete(new String(result));
            }
        };
        socket.readUntil("|".getBytes(), rcb);
    }

    private void onReadComplete(String result) {
        if ("ROGER".equals(result)) {
            latch.countDown();
        }
        assertEquals("ROGER", result);
    }

}

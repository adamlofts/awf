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

import com.google.common.collect.Maps;

import org.apache.awf.io.IOLoop;
import org.apache.awf.util.Closeables;
import org.apache.awf.web.Application;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpProtocol;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * User: slemesle
 * Date: 11/09/11
 * Time: 12:35
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {Closeables.class})
@PowerMockIgnore("javax.management.*")
public class HttpProtocolTest {

    private IOLoop ioLoop;
    private HttpProtocol protocol;
    private SelectionKey key;
    private SocketChannel socketChannel;


    @Before
    public void beforeTests(){

        ioLoop = Mockito.mock(IOLoop.class);
        socketChannel = Mockito.mock(SocketChannel.class);
        key = new MySelectionKey(socketChannel);
        PowerMockito.mockStatic(Closeables.class);
        protocol = new HttpProtocol(ioLoop, new Application(Maps.<String, RequestHandler>newHashMap()));
    }

    @Test
    public void testHandleReadReachEOF() throws Exception {

        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        key.attach(byteBuffer);

        // See what happens when read returns -1
        Mockito.when(socketChannel.read(byteBuffer)).thenReturn(-1);

        protocol.handleRead(key);

        Mockito.verify(socketChannel).read(byteBuffer);

        // CloseQuietly should have been called for this channel EOF
        PowerMockito.verifyStatic(Mockito.times(1));
        Closeables.closeQuietly(ioLoop, socketChannel);
    }


    /**
     * Since did not succeed in mocking final fields
     * here is a short mock for the SelectionKey
     */
    class MySelectionKey extends SelectionKey {

        SelectableChannel channel;

        MySelectionKey(SelectableChannel channel) {
            super();
            this.channel = channel;
        }

        @Override
        public SelectableChannel channel() {
            return channel;
        }

        @Override
        public Selector selector() {
            return null;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void cancel() {
            // Nothing Todo
        }

        @Override
        public int interestOps() {
            return 0;  
        }

        @Override
        public SelectionKey interestOps(int i) {
            return this;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }
}

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
package org.apache.awf.util;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.apache.awf.io.IOHandler;
import org.apache.awf.io.IOLoop;
import org.apache.awf.web.AsyncCallback;

public class AcceptUtil {

    public static void accept(ServerSocketChannel server, final AsyncCallback cb) {
        accept(IOLoop.INSTANCE, server, cb);
    }

    public static void accept(IOLoop ioLoop, ServerSocketChannel server, final AsyncCallback cb) {
        ioLoop.addHandler(server, new AcceptingIOHandler() {
            public void handleAccept(SelectionKey key) {
                cb.onCallback();
            }
        }, SelectionKey.OP_ACCEPT, null);
    }

    private static abstract class AcceptingIOHandler implements IOHandler {

        public void handleConnect(SelectionKey key) throws IOException {
        }

        public void handleRead(SelectionKey key) throws IOException {
        }

        public void handleWrite(SelectionKey key) {
        }

    }

}

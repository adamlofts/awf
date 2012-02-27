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
package org.apache.awf.web.http.client;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Stack;

import org.apache.awf.io.AsynchronousSocket;

/**
 * ThreadSafe socket pool
 */
public class SocketPool {

    final static int MAX_SIZE = 10;
    final static int MAX_AGE_MSEC = 1000 * 20; // 20 seconds
    
    private static SocketPool pool;
    
    public static SocketPool getDefault() {
        if (pool == null) {
            pool = new SocketPool();
        }
        return pool;
    }
    
    class AddressTuple {
        private String host;
        private int port;
        
        public AddressTuple(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        @Override
        public int hashCode() {
            return this.host.hashCode() + port;
        }
        
        @Override
        public boolean equals(Object obj) {
            AddressTuple other = (AddressTuple) obj;
            return other.host.equals(host) && other.port == port;
        }
    }
    
    private Dictionary<AddressTuple, Stack<AsynchronousSocket>> connections;
    
    /**
     * Create an instance of this type.
     */
    public SocketPool() {
        connections = new Hashtable<SocketPool.AddressTuple, Stack<AsynchronousSocket>>();
    }
    
    public void returnSocket(final String host, final int port, AsynchronousSocket socket) {
        AddressTuple tuple = new AddressTuple(host, port);
        
        synchronized (connections) {
            Stack<AsynchronousSocket> stack = connections.get(tuple);
            if (stack == null) {
                stack = new Stack<AsynchronousSocket>();
                connections.put(tuple, stack);
            }
            
            // If the stack has room then add the socket and return
            if (stack.size() < MAX_SIZE) {
                stack.push(socket);
                return;
            }
        }
        
        // If reached then the stack is full, close the socket
        socket.close();
    }

    private AsynchronousSocket popSocket(final AddressTuple tuple) {
        synchronized (connections) {
            Stack<AsynchronousSocket> stack = connections.get(tuple);
            if (stack == null) {
                return null;
            }
            
            if (stack.isEmpty()) {
                return null;
            }
            AsynchronousSocket socket = stack.pop();
            return socket;
        }
    }
    
    public AsynchronousSocket getPooledSocket(final String host, final int port) {
        final AddressTuple tuple = new AddressTuple(host, port);
        
        while (true) {
            AsynchronousSocket socket = popSocket(tuple);
            if (socket == null) {
                return null;
            }
            
            boolean isWithinTimeout = System.currentTimeMillis() - socket.getConnectedTime() < MAX_AGE_MSEC;
            if (socket.isConnected() && isWithinTimeout) {
                return socket;
            }
            
            // If the socket is no longer connected then quietly dispose of it
            socket.close();
        }
    }

}

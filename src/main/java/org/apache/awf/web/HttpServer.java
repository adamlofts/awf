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
package org.apache.awf.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.apache.awf.configuration.AnnotationsScanner;
import org.apache.awf.configuration.Configuration;
import org.apache.awf.io.IOLoop;
import org.apache.awf.io.IOLoopObserver;
import org.apache.awf.util.Closeables;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {

    private final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private static final int LATCH_TIMEOUT = 500;
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    private ServerSocketChannel serverChannel;
    private final List<IOLoop> ioLoops = Lists.newLinkedList();

    private final Configuration configuration;

    private final Application application;

    public HttpServer(Configuration configuration) {
        this.configuration = configuration;

        // Support manually mapped handlers
        Map<String, RequestHandler> handlers = configuration.getHandlerMap();
        // Add Annotated classes
        handlers.putAll(new AnnotationsScanner().findHandlers(configuration.getHandlerPackage()));
        application = new Application(handlers);
        application.setStaticContentDir(configuration.getStaticDirectory());
        application.setConfiguration(configuration);
    }
    
    public HttpServer(Configuration configuration, Application application) {
    	this.application = application;
    	this.configuration = configuration;
    }

    /**
     * If you want to run AWF with multiple threads first invoke
     * {@link #bind(int)} then {@link #start(int)} instead of
     * {@link #listen(int)} (listen starts an HTTP server on a single thread
     * with the default IOLoop instance: {@code IOLoop.INSTANCE}).
     */
    public void listen(int port) {
        bind(port);
        ioLoops.add(IOLoop.INSTANCE);
        registerHandler(IOLoop.INSTANCE, new HttpProtocol(application));
    }

    public void bind(int port) {
        if (port <= MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid port number. Valid range: [" + MIN_PORT_NUMBER + ", "
                    + MAX_PORT_NUMBER + ")");
        }
        try {
            serverChannel = ServerSocketChannel.open();

            boolean reuse = serverChannel.socket().getReuseAddress();
            if (!reuse) {
                logger.info("Enabling SO_REUSEADDR (was disabled)");
                serverChannel.socket().setReuseAddress(true);
            }
            serverChannel.configureBlocking(false);
        } catch (IOException e) {
            logger.error("Error creating ServerSocketChannel: {}", e);
        }

        InetSocketAddress endpoint = new InetSocketAddress(port);
        try {
            serverChannel.socket().bind(endpoint);
        } catch (IOException e) {
            logger.error("Could not bind socket: {}", e);
        }
    }

    public void start(int numThreads) {

        observer = new LatchObserver(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final IOLoop ioLoop = new IOLoop();
            ioLoops.add(ioLoop);
            final HttpProtocol protocol = new HttpProtocol(ioLoop, application);
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {

                    registerHandler(ioLoop, protocol);
                    ioLoop.start(observer);
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
        try {
            observer.started.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for all IOLoop to start", e);
        }
    }

    /**
     * Unbinds the port and shutdown the HTTP server using a callback to execute
     * the stop from the IOLoop thread
     * 
     */
    public void stop() {
        logger.debug("Stopping HTTP server");

        for (final IOLoop ioLoop : ioLoops) {
            // Use a callback to stop the loops from theire Threads
            ioLoop.addCallback(new AsyncCallback() {
                @Override
                public void onCallback() {
                    Closeables.closeQuietly(ioLoop, serverChannel);
                    ioLoop.stop();
                }
            });
        }
        try {
            observer.stopped.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for all IOLoop to stop", e);
        }
    }

    private void registerHandler(IOLoop ioLoop, HttpProtocol protocol) {
        ioLoop.addHandler(serverChannel, protocol, SelectionKey.OP_ACCEPT, null);
    }

    /**
     * Added for test purposes.
     * 
     * @return a <code>List</code> of current <code>IOLoop</code>s.
     */
    protected List<IOLoop> getIoLoops() {
        return ioLoops;
    }


    private volatile LatchObserver observer;

    class LatchObserver implements IOLoopObserver {

        final CountDownLatch started, stopped;

        LatchObserver(int nbThread) {
            this.started = new CountDownLatch(nbThread);
            this.stopped = new CountDownLatch(nbThread);
        }

        public void onStart(IOLoop loop) {

            started.countDown();
        }

        public void onStopped(IOLoop loop) {
            stopped.countDown();
        }


    };


}

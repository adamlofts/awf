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

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.awf.io.callback.CallbackManager;
import org.apache.awf.io.callback.JMXDebuggableCallbackManager;
import org.apache.awf.io.timeout.JMXDebuggableTimeoutManager;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.io.timeout.TimeoutManager;
import org.apache.awf.util.MXBeanUtil;
import org.apache.awf.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.collect.Collections2.transform;

public class IOLoop implements IOLoopMXBean {

    private static final AtomicInteger sequence = new AtomicInteger();
    
    /*
     * IOLoop singleton to use for convenience (otherwise you would have to pass
     * around the IOLoop instance explicitly, now you can simply use
     * IOLoop.INSTANCE)
     */
    public static final IOLoop INSTANCE = new IOLoop();

    private volatile boolean running = false;

    private final Logger logger = LoggerFactory.getLogger(IOLoop.class);

    private Selector selector;

    private final Map<SelectableChannel, IOHandler> handlers = Maps.newHashMap();

    private final TimeoutManager tm = new JMXDebuggableTimeoutManager();
    private final CallbackManager cm = new JMXDebuggableCallbackManager();

    private final int ID;

    private IOLoopObserver observer;


    public IOLoop() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            logger.error("Could not open selector: {}", e.getMessage());
        }
        ID = sequence.incrementAndGet();
        MXBeanUtil.registerMXBean(this, "IOLoop",this.getClass().getSimpleName()+"-"+ID);
    }

    /**
     * Start the IOLoop using a callback observer to
     * @param cb
     */
    public void start(IOLoopObserver cb){
        this.observer = cb;
        start();
    }


    /**
     * Start the io loop. The thread that invokes this method will be blocked
     * (until {@link IOLoop#stop} is invoked) and will be the io loop thread.
     */
    public void start() {
        Thread.currentThread().setName("I/O-LOOP" + ID);
        running = true;
        if(this.observer != null){
            this.observer.onStart(this);
        }
        long selectorTimeout = 250; // 250 ms
        while (running) {
            try {
                if (selector.select(selectorTimeout) == 0) {
                    long ms = tm.execute();
                    selectorTimeout = Math.min(ms, /* selectorTimeout */250);
                    if (cm.execute()) {
                        selectorTimeout = 1;
                    }
                    continue;
                }

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    IOHandler handler = handlers.get(key.channel());
                    if (key.isAcceptable()) {
                        handler.handleAccept(key);
                    }
                    if (key.isConnectable()) {
                        handler.handleConnect(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        handler.handleRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        handler.handleWrite(key);
                    }
                    keys.remove();
                }
                long ms = tm.execute();
                selectorTimeout = Math.min(ms, /* selectorTimeout */250);
                if (cm.execute()) {
                    selectorTimeout = 1;
                }
            } catch (IOException e) {
                logger.error("IOException received in IOLoop: {}", e);
            } catch (CancelledKeyException e) {
                logger.error("CancelledKeyException received in IOLoop: {}", e);
            }
        }
        // Call observer if there is one
        if(this.observer != null){
            this.observer.onStopped(this);
        }

    }

    /**
     * Stop the io loop and release the thread (io loop thread) that invoked the
     * {@link IOLoop#start} method.
     */
    public void stop() {
        running = false;
        logger.debug("Stopping IOLoop...");
    }

    /**
     * Registers a new {@code IOHandler} with this {@code IOLoop}.
     * 
     * @param channel The {@code SelectableChannel}
     * @param handler {@code IOHandler that will receive the io callbacks.}
     * @param interestOps See {@link SelectionKey} for valid values. (Xor for
     *            multiple interests).
     * @param attachment The {@code attachment} that will be accessible from the
     *            returning {@code SelectionKey}s attachment.
     * 
     */
    public SelectionKey addHandler(SelectableChannel channel, IOHandler handler, int interestOps, Object attachment) {
        handlers.put(channel, handler);
        return registerChannel(channel, interestOps, attachment);
    }

    /**
     * Unregisters the previously registered {@code IOHandler}.
     * 
     * @param channel The {@code SelectableChannel} that was registered with a
     *            user defined {@code IOHandler}
     */
    public void removeHandler(SelectableChannel channel) {
        handlers.remove(channel);
    }

    /**
     * Update an earlier registered {@code SelectableChannel}
     * 
     * @param channel The {@code SelectableChannel}
     * @param newInterestOps The complete new set of interest operations.
     */
    public void updateHandler(SelectableChannel channel, int newInterestOps) {
        if (handlers.containsKey(channel)) {
            channel.keyFor(selector).interestOps(newInterestOps);
        } else {
            logger.warn("Tried to update interestOps for an unknown SelectableChannel.");
        }
    }

    /**
     * 
     * @param channel
     * @param interestOps
     * @param attachment
     * @return
     */
    private SelectionKey registerChannel(SelectableChannel channel, int interestOps, Object attachment) {
        try {
            return channel.register(selector, interestOps, attachment);
        } catch (ClosedChannelException e) {
            removeHandler(channel);
            logger.error("Could not register channel: {}", e.getMessage());
        }
        return null;
    }

    public void addKeepAliveTimeout(SelectableChannel channel, Timeout keepAliveTimeout) {
        tm.addKeepAliveTimeout(channel, keepAliveTimeout);
    }

    public boolean hasKeepAliveTimeout(SelectableChannel channel) {
        return tm.hasKeepAliveTimeout(channel);
    }

    public void addTimeout(Timeout timeout) {
        tm.addTimeout(timeout);
    }

    /**
     * The callback will be invoked in the next iteration in the io loop. This
     * is the only thread safe method that is exposed by AWF. This is a
     * convenient way to return control to the io loop.
     */
    public void addCallback(AsyncCallback callback) {
        cm.addCallback(callback);
    }

    // implements IOLoopMXBean
    @Override
    public int getNumberOfRegisteredIOHandlers() {
        return handlers.size();
    }

    @Override
    public List<String> getRegisteredIOHandlers() {
        Map<SelectableChannel, IOHandler> defensive = new HashMap<SelectableChannel, IOHandler>(handlers);
        Collection<String> readables = transform(defensive.values(), new Function<IOHandler, String>() {
            @Override
            public String apply(IOHandler handler) {
                return handler.toString();
            }
        });
        return Lists.newLinkedList(readables);
    }

    /**
     * Checks whether this IOLoop is running or not.
     * 
     * @return <code>true</code> if running; <code>false</code> otherwise.
     */
    public boolean isRunning() {
        return running;
    }
}

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
package org.apache.awf.io.timeout;

import java.nio.channels.SelectableChannel;

import org.apache.awf.io.IOLoop;
import org.apache.awf.util.Closeables;
import org.apache.awf.web.AsyncCallback;


public class Timeout {

	private final long timeout;
	private final AsyncCallback cb;
	private boolean cancelled = false;
	
	public Timeout(long timeout, AsyncCallback cb) {
		this.timeout = timeout;
		this.cb = cb;
	}

	public long getTimeout() {
		return timeout;
	}
	
	public void cancel() {
		cancelled = true;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}

	public AsyncCallback getCallback() {
		return cancelled ? AsyncCallback.nopCb : cb;
	}
	
	public static Timeout newKeepAliveTimeout(final IOLoop ioLoop, final SelectableChannel clientChannel, long keepAliveTimeout) {
		return new Timeout(
				System.currentTimeMillis() + keepAliveTimeout,
				new AsyncCallback() { public void onCallback() { Closeables.closeQuietly(ioLoop, clientChannel); } }
		);
	}

}

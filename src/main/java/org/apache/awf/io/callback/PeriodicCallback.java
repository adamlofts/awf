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
package org.apache.awf.io.callback;

import org.apache.awf.io.IOLoop;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.web.AsyncCallback;

public class PeriodicCallback {
	
	private final IOLoop ioLoop;
	private final AsyncCallback cb;
	private final long period;
	private boolean active = true;
	
	/** 
	 * A periodic callback that will execute its callback once every period.
	 * @param cb 
	 * @param period The period in ms
	 */
	public PeriodicCallback(AsyncCallback cb, long period) {
		this(IOLoop.INSTANCE, cb, period);
	}
	
	public PeriodicCallback(IOLoop ioLoop, AsyncCallback cb, long period) {
		this.ioLoop = ioLoop;
		this.cb = cb;
		this.period = period;
	}
	
	/**
	 * Start the {@code PeriodicCallback}
	 */
	public void start() {
		ioLoop.addTimeout(
				new Timeout(
						System.currentTimeMillis() + period, 
						new AsyncCallback() { @Override public void onCallback() { run(); }}
				)
		);
	}
	
	private void run() {
		if (active) {
			cb.onCallback();
			start();	// reschedule
		}
	}
	
	/**
	 * Cancel the {@code PeriodicCallback}. (No way to resume the cancellation, you will need to create a new
	 * {@code PeriodicCallback}).
	 */
	public void cancel() {
		this.active = false;
	}
	
}

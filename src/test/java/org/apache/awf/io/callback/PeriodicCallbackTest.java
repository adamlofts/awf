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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.awf.io.IOLoop;
import org.apache.awf.web.AsyncCallback;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PeriodicCallbackTest {
	
	@Test
	public void testPeriodicCallback() throws InterruptedException {
		// start the IOLoop from a new thread so we dont block this test.
		new Thread(new Runnable() {

			@Override public void run() { IOLoop.INSTANCE.start(); }
		
		}).start();
		
		final CountDownLatch latch = new CountDownLatch(200);
		long period = 10; // 10ms (=> ~100times / s)
		AsyncCallback cb = new AsyncCallback() {
			@Override public void onCallback() { latch.countDown(); }
		};
		final PeriodicCallback pcb = new PeriodicCallback(cb, period);
		IOLoop.INSTANCE.addCallback(new AsyncCallback() { public void onCallback() { pcb.start(); }});
		
		latch.await(5, TimeUnit.SECONDS);
		pcb.cancel();
		IOLoop.INSTANCE.stop();
		// TODO wait?
		assertEquals(0, latch.getCount());
	}
	
}

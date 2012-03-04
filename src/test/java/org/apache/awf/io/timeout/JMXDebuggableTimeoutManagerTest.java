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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;

import org.apache.awf.io.timeout.JMXDebuggableTimeoutManager;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.web.AsyncCallback;
import org.junit.Test;

import com.google.common.collect.Maps;

public class JMXDebuggableTimeoutManagerTest {

	private final JMXDebuggableTimeoutManager tm = new JMXDebuggableTimeoutManager();
	
	@Test
	public void timeoutManagerTest() throws InterruptedException {
		final long now = System.currentTimeMillis();
		MockChannel c1 = new MockChannel();
		MockChannel c2 = new MockChannel();
		MockChannel c3 = new MockChannel();

		addNopTimeout(now);
		addNopTimeout(now);
		addNopTimeout(now);
		addNopTimeout(now+1);
		addNopTimeout(now+2);
		addNopTimeout(now+1000);
		addNopTimeout(now+1200);
		addNopTimeout(now+1400);
		
		addNopKeepAliveTimeout(c1, now);
		addNopKeepAliveTimeout(c2, now);
		addNopKeepAliveTimeout(c3, now+1);
		
		assertEquals(11, tm.getNumberOfTimeouts());
		assertEquals(3, tm.getNumberOfKeepAliveTimeouts());

		Thread.sleep(200);
	
		tm.execute();
		assertEquals(3, tm.getNumberOfTimeouts());
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts());
	
		Thread.sleep(2000);
		tm.execute();
		assertEquals(0, tm.getNumberOfTimeouts());
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts());
	}
	
	private void addNopTimeout(long timeout) {
		tm.addTimeout(new Timeout(timeout, new AsyncCallback() {
			@Override public void onCallback() { /*nop*/}
		}));	
	}

	private void addNopKeepAliveTimeout(SelectableChannel channel, long timeout) {
		tm.addKeepAliveTimeout(channel, new Timeout(timeout, new AsyncCallback() {
			@Override public void onCallback() { /*nop*/ }
		}));
	}
	
	@Test
	public void addTimeoutDuringTimeoutExecution() throws InterruptedException {
		final long now = System.currentTimeMillis();
		addRecursiveTimeout(now);
		addRecursiveTimeout(now+10);
		addRecursiveTimeout(now+20);
		
		assertEquals(3, tm.getNumberOfTimeouts());
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts());

		Thread.sleep(50);
		long ms = tm.execute();
		assertTrue(ms != Long.MAX_VALUE);
		
		assertEquals(3, tm.getNumberOfTimeouts());
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts());
		
		Thread.sleep(50);
		tm.execute();
		Thread.sleep(50);
		tm.execute();
		Thread.sleep(50);
		tm.execute();
		
		assertEquals(0, tm.getNumberOfTimeouts());
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts());
	}
	
	private void addRecursiveTimeout(final long timeout) {
		final Timeout t = new Timeout(timeout, new AsyncCallback() {
			@Override public void onCallback() { addNopTimeout(System.currentTimeMillis()); }
		});
		tm.addTimeout(t);	
	}
	
	private class MockChannel extends SelectableChannel {

		@Override
		public Object blockingLock() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SelectableChannel configureBlocking(boolean block)
		throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isBlocking() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isRegistered() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public SelectionKey keyFor(Selector sel) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SelectorProvider provider() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SelectionKey register(Selector sel, int ops, Object att)
		throws ClosedChannelException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int validOps() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		protected void implCloseChannel() throws IOException {
			// TODO Auto-generated method stub

		}
		
	}
	
	@Test
	public void testAddTimeoutsThatHasTheSameDeadline() throws InterruptedException {
		assertEquals(0, tm.getNumberOfKeepAliveTimeouts() + tm.getNumberOfTimeouts());
		final int N = 1000;
		final long now = System.currentTimeMillis();
		final Map<Integer, Boolean> register = Maps.newHashMap();
		for (int i = 0; i < N; i++) {
			final int j = i;
			Timeout t = new Timeout(
					now + 10, 
					new AsyncCallback() { public void onCallback() { register.put(j, true); }}
			);
			register.put(j, false);
			tm.addTimeout(t);
		}
		assertEquals(N, register.size());
		assertEquals(N, tm.getNumberOfTimeouts());
		
		Thread.sleep(100);
		tm.execute();
		assertEquals(N, register.size());
		assertEquals(0, tm.getNumberOfTimeouts());
		for (Boolean timeoutTriggered: register.values()) {
			assertTrue(timeoutTriggered);
		}
	}

}

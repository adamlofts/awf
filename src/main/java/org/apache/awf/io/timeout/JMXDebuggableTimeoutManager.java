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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.awf.util.MXBeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;


public class JMXDebuggableTimeoutManager implements TimeoutManager, TimeoutManagerMXBean {

	private final Logger logger = LoggerFactory.getLogger(JMXDebuggableTimeoutManager.class);

	private final TreeSet<Timeout> timeouts = Sets.newTreeSet(new TimeoutComparator()); 
	private final TreeMultiset<DecoratedTimeout> keepAliveTimeouts = TreeMultiset.create();
	private final Map<SelectableChannel, DecoratedTimeout> index = Maps.newHashMap();
    private static final AtomicInteger sequence = new AtomicInteger(1);

    public JMXDebuggableTimeoutManager()
	{ 	// instance initialization block
		MXBeanUtil.registerMXBean(this, "TimeoutManager",this.getClass().getSimpleName()+"-"+sequence.incrementAndGet());
	}

	@Override
	public void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout) {
		logger.debug("added keep-alive timeout: {}", timeout);
		DecoratedTimeout oldTimeout = index.get(channel);
		if (oldTimeout != null) {
			keepAliveTimeouts.remove(oldTimeout);
		}
		DecoratedTimeout decorated = new DecoratedTimeout(channel, timeout);
		keepAliveTimeouts.add(decorated);
		index.put(channel, decorated);
	}

	@Override
	public void addTimeout(Timeout timeout) {
		logger.debug("added generic timeout: {}", timeout);
		timeouts.add(timeout);
	}

	@Override
	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return index.containsKey(channel);
	}

	@Override
	public long execute() {
		return Math.min(executeKeepAliveTimeouts(), executeTimeouts());
	}
	
	private long executeKeepAliveTimeouts() {
		// makes a defensive copy to avoid (1) CME (new timeouts are added this iteration) and (2) IO starvation.
		TreeMultiset<DecoratedTimeout> defensive = TreeMultiset.create(keepAliveTimeouts);
		Iterator<DecoratedTimeout> iter = defensive.iterator();
		final long now = System.currentTimeMillis();
		while (iter.hasNext()) {
			DecoratedTimeout candidate = iter.next();
			if (candidate.timeout.getTimeout() > now) { break; }
			candidate.timeout.getCallback().onCallback();
			index.remove(candidate.channel);
			iter.remove();
			keepAliveTimeouts.remove(candidate);
			logger.debug("Keep-alive timeout triggered: {}", candidate.timeout);
		}
		return keepAliveTimeouts.isEmpty() ? Long.MAX_VALUE : Math.max(1, keepAliveTimeouts.iterator().next().timeout.getTimeout() - now);
	}
	
	private long executeTimeouts() {
		// makes a defensive copy to avoid (1) CME (new timeouts are added this iteration) and (2) IO starvation.
		TreeSet<Timeout> defensive = new TreeSet<Timeout>(timeouts); /*Sets.newTreeSet(timeouts);*/
		Iterator<Timeout> iter = defensive.iterator();
		final long now = System.currentTimeMillis();
		while (iter.hasNext()) {
			Timeout candidate = iter.next();
			if (candidate.getTimeout() > now) { break; }
			candidate.getCallback().onCallback();
			iter.remove();
			timeouts.remove(candidate);
			logger.debug("Timeout triggered: {}", candidate);
		}
		return timeouts.isEmpty() ? Long.MAX_VALUE : Math.max(1, timeouts.iterator().next().getTimeout() - now);
	}

	// implements TimoutMXBean
	@Override
	public int getNumberOfKeepAliveTimeouts() {
		return index.size();
	}

	@Override
	public int getNumberOfTimeouts() {
		return keepAliveTimeouts.size() + timeouts.size();
	}

	private class DecoratedTimeout implements Comparable<DecoratedTimeout> {

		public final SelectableChannel channel;
		public final Timeout timeout;

		public DecoratedTimeout(SelectableChannel channel, Timeout timeout) {
			this.channel = channel;
			this.timeout = timeout;
		}

		@Override
		public int compareTo(DecoratedTimeout that) {
			long diff = timeout.getTimeout() - that.timeout.getTimeout();
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1; 
			} 
			if (channel != null && that.channel != null) {
				return channel.hashCode() - that.channel.hashCode(); 
			} else if (channel == null && that.channel != null){
				return -1;
			} else if (channel != null && that.channel == null){
				return -1;
			} else {
				return 0;
			}
		}
		
	}
	
	private class TimeoutComparator implements Comparator<Timeout> {

		@Override
		public int compare(Timeout lhs, Timeout rhs) {
			if (lhs == rhs) { 
				return 0;
			}
			long diff = lhs.getTimeout() - rhs.getTimeout();
			if (diff <= 0) {
				return -1;
			}
			return 1;	/// else if (diff > 0) { 
		}
		
	}

}

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
package org.apache.awf.io.buffer;

import static org.junit.Assert.assertEquals;

import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.junit.Before;
import org.junit.Test;

public class DynamicByteBufferTest {

	private DynamicByteBuffer dbb;
	private static final int INITIAL_CAPACITY = 10;	// bytes
	
	@Before
	public void allocation() {
		this.dbb = DynamicByteBuffer.allocate(INITIAL_CAPACITY);
	}
	
	@Test
	public void testAllocation() {
		assertInternalState(INITIAL_CAPACITY, 0, INITIAL_CAPACITY, INITIAL_CAPACITY);
	}
	
	private void assertInternalState(int expectedCapacity, int expectedPosition, int expectedLimit, int arrayLength) {
		assertEquals(expectedCapacity, dbb.capacity());
		assertEquals(expectedPosition, dbb.position());
		assertEquals(expectedLimit, dbb.limit());
		assertEquals(arrayLength, dbb.array().length);
	}
	
	@Test
	public void testNoReallactionPut() {
		byte[] data = new byte[] {'q', 'w', 'e', 'r', 't', 'y'};
		dbb.put(data);
		assertInternalState(INITIAL_CAPACITY, data.length, INITIAL_CAPACITY, INITIAL_CAPACITY);
	}
	
	@Test
	public void testReallocationTriggeredPut() {
		byte[] data = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A'};
		dbb.put(data);
		assertInternalState(16, 11, 16, 16);
		
		dbb.put(data);
		assertInternalState(33, 22, 33, 33);
		
		dbb.put(data);
		dbb.put(data);
		assertInternalState(66, 44, 66, 66);
	}
	
	@Test
	public void testPrepend() {
		byte[] data = new byte[] {'[', 'q', 'w', 'e', 'r', 't', 'y', ']'};
		dbb.put(data);
		
		String initial = "|HTTP/1.1 200 OK|";
		int initialLength = initial.length();
		dbb.prepend(initial);
		
		int expectedCapacity = data.length + initialLength;
		int expectedPosition = data.length + initialLength;
		int expectedLimit 	 = data.length + initialLength;
		int arrayLength 	 = data.length + initialLength;
		assertInternalState(expectedCapacity, expectedPosition, expectedLimit, arrayLength);
		
		dbb.put(data);
		expectedCapacity = 49;	
		expectedPosition += data.length;
		expectedLimit 	 = 49;
		arrayLength 	 = 49;
		assertInternalState(expectedCapacity, expectedPosition, expectedLimit, arrayLength);
	}

	@Test
	public void testReallocationLimit() {
		byte[] data = "0123456".getBytes();
		dbb.put(data);
		assertInternalState(10, 7, 10, 10);
		dbb.put("0123456789".getBytes());
		assertInternalState(25, 17, 25, 25);
	}
	
}

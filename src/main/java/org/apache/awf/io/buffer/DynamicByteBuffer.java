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

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class DynamicByteBuffer {
	
	private final static Logger logger = LoggerFactory.getLogger(DynamicByteBuffer.class);

	private ByteBuffer backend;

	private DynamicByteBuffer(ByteBuffer bb) { 	
		this.backend = bb;
	}
	
	/**
	 * Allocate a new {@code DynamicByteBuffer} that will be using a {@code ByteBuffer} internally.
	 * @param capacity initial capacity
	 */
	public static DynamicByteBuffer allocate(int capacity) {
		return new DynamicByteBuffer(ByteBuffer.allocate(capacity));
	}

	/**
	 * Append the data. Will reallocate if needed.
	 */
	public void put(byte[] src) {
		ensureCapacity(src.length);
		backend.put(src);
	}
	
	/**
	 * Append the bytes from the given src. Will reallocate if needed.
	 */
	public void put(ByteBuffer src) {
		ensureCapacity(src.limit());
		backend.put(src);
	}


    /**
     * Append count bytes in the given byte array start at array position
     * @param array     byte array to copy
     * @param position  start position
     * @param count     bytes to copy count
     */
    public void put(byte[] array, int position, int count) {
        ensureCapacity(count);
		backend.put(array, position, count);
    }

	/**
	 * Prepend the data. Will reallocate if needed.
	 */
	public void prepend(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		int newSize = bytes.length + backend.position();
		byte[] newBuffer = new byte[newSize];
		System.arraycopy(bytes, 0, newBuffer, 0, bytes.length);	// initial line and headers
		System.arraycopy(backend.array(), 0, newBuffer, bytes.length, backend.position()); // body
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(newSize);
	}

	/**
	 * Ensures that its safe to append size data to backend.
	 * @param size The size of the data that is about to be appended.
	 */
	private void ensureCapacity(int size) {
		int remaining = backend.remaining();
		if (size > remaining) {
			logger.debug("allocating new DynamicByteBuffer, old capacity {}: ", backend.capacity());
            int missing = size - remaining;
			int newSize =  (int) ((backend.capacity() + missing) * 1.5);
			reallocate(newSize);
		}
	}

	// Preserves position.
	private void reallocate(int newCapacity) {
		int oldPosition = backend.position();
		byte[] newBuffer = new byte[newCapacity];
		System.arraycopy(backend.array(), 0, newBuffer, 0, backend.position());
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(oldPosition);
		logger.debug("allocated new DynamicByteBufer, new capacity: {}", backend.capacity());
	}

	/**
	 * Returns the {@code ByteBuffer} that is used internally by this {@code DynamicByteBufer}.
	 * Changes made to the returned {@code ByteBuffer} will be incur modifications in this {@code DynamicByteBufer}.
	 */
	public ByteBuffer getByteBuffer() {
		return backend;
	}

	/**
	 * See {@link ByteBuffer#get(byte[], int, int)}
	 */
	public void get(byte[] dst, int offset, int length) {
		backend.get(dst, offset, length);
	}

	public void position(int newPosition) {
		backend.position(newPosition);
	}

	/**
	 * See {@link ByteBuffer#flip}
	 */
	public void flip() {
		backend.flip();
	}

	/**
	 * See {@link ByteBuffer#limit}
	 */
	public int limit() {
		return backend.limit();
	}

	/**
	 * See {@link ByteBuffer#position}
	 */
	public int position() {
		return backend.position();
	}

	/**
	 * See {@link ByteBuffer#array}
	 */
	public byte[] array() {
		return backend.array();
	}

	/**
	 * See {@link ByteBuffer#capacity}
	 */
	public int capacity() {
		return backend.capacity();
	}

	/**
	 * See {@link ByteBuffer#hasRemaining}
	 */
	public boolean hasRemaining() {
		return backend.hasRemaining();
	}

	/**
	 * See {@link ByteBuffer#compact}
	 */
	public DynamicByteBuffer compact() {
		backend.compact();
		return this;
	}

	/**
	 * See {@link ByteBuffer#clear}
	 */
	public DynamicByteBuffer clear() {
		backend.clear();
		return this;
	}
}

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
package org.apache.awf.web.http.protocol;

import org.apache.awf.web.http.protocol.HttpStatus;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for {@link HttpStatus}.
 */
public class HttpStatusTest {

    private static int STATUS_CODE_COUNT = 38;

    @Test
    public void testCount() {
        assertEquals(STATUS_CODE_COUNT, HttpStatus.values().length);
    }

    @Test
    public void testCode() {

        for (final HttpStatus status : HttpStatus.values()) {
            if (status.name().startsWith("SUCCESS")) {
                assertTrue("Incorrect: " + status.name(), status.code() >= 100 && status.code() < 300);
            } else if (status.name().startsWith("REDIRECTION")) {
                assertTrue("Incorrect: " + status.name(), status.code() >= 300 && status.code() < 400);
            } else if (status.name().startsWith("CLIENT_ERROR")) {
                assertTrue("Incorrect: " + status.name(), status.code() >= 400 && status.code() < 500);
            } else if (status.name().startsWith("SERVER_ERROR")) {
                assertTrue("Incorrect: " + status.name(), status.code() >= 500 && status.code() < 600);
            } else {
                assertTrue("Unknown: " + status.name(), false);
            }
        }
    }

    @Test
    public void testLine() {

        for (final HttpStatus status : HttpStatus.values()) {
            assertNotNull(status.line());
            assertTrue("Incorrect: " + status.name(), status.line().startsWith("HTTP/1.1 " + status.code()));
        }
    }
}

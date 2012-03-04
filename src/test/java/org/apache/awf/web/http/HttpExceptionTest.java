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
package org.apache.awf.web.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.awf.web.http.HttpException;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.junit.Test;

/**
 * Test cases for {@link HttpException}.
 */
public class HttpExceptionTest {

    @Test
    public void testHttpException() {

        HttpException exception = new HttpException(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);

        assertEquals(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR, exception.getStatus());
        assertTrue(exception.getMessage().isEmpty());
    }

    @Test
    public void testHttpExceptionWithMessage() {

        HttpException exception = new HttpException(HttpStatus.CLIENT_ERROR_NOT_FOUND, "testMessage");

        assertEquals(HttpStatus.CLIENT_ERROR_NOT_FOUND, exception.getStatus());
        assertEquals("testMessage", exception.getMessage());
    }
}

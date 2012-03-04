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
package org.apache.awf.web.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.awf.annotation.Asynchronous;
import org.apache.awf.annotation.Authenticated;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.HttpResponse;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.junit.Test;

public class RequestHandlerTest {

    static class RequestHandler1 extends RequestHandler {

        @Override
        @Asynchronous
        @Authenticated
        public void get(HttpRequest request, HttpResponse response) {
        }
    }

    static class RequestHandler2 extends RequestHandler {

        @Override
        public void get(HttpRequest request, HttpResponse response) {
        }

        @Override
        @Asynchronous
        @Authenticated
        public void post(HttpRequest request, HttpResponse response) {
        }

    }

    private class TestRequestHandler extends RequestHandler {
        // Do nothing.
    }

    @Test
    public void testAsynchronousAnnotations() {

        RequestHandler rh1 = new RequestHandler1();
        RequestHandler rh2 = new RequestHandler2();

        assertTrue(rh1.isMethodAsynchronous(HttpVerb.GET));

        assertFalse(rh2.isMethodAsynchronous(HttpVerb.GET));
        assertTrue(rh2.isMethodAsynchronous(HttpVerb.POST));
    }

    @Test
    public void testAuthenticatedAnnotations() {
        RequestHandler rh1 = new RequestHandler1();
        RequestHandler rh2 = new RequestHandler2();

        assertTrue(rh1.isMethodAuthenticated(HttpVerb.GET));
        assertFalse(rh1.isMethodAuthenticated(HttpVerb.POST));
        assertFalse(rh1.isMethodAuthenticated(HttpVerb.DELETE));

        assertFalse(rh2.isMethodAuthenticated(HttpVerb.GET));
        assertFalse(rh2.isMethodAuthenticated(HttpVerb.PUT));
        assertTrue(rh2.isMethodAuthenticated(HttpVerb.POST));
    }

    @Test
    public void testGet() {

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        new TestRequestHandler().get(request, response);
        verify(response, times(1)).setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(response, times(1)).write("");
    }

    @Test
    public void testPost() {

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        new TestRequestHandler().post(request, response);
        verify(response, times(1)).setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(response, times(1)).write("");
    }

    @Test
    public void testPut() {

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        new TestRequestHandler().put(request, response);
        verify(response, times(1)).setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(response, times(1)).write("");
    }

    @Test
    public void testDelete() {

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        new TestRequestHandler().delete(request, response);
        verify(response, times(1)).setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(response, times(1)).write("");
    }

    @Test
    public void testHead() {

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        new TestRequestHandler().head(request, response);
        verify(response, times(1)).setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(response, times(1)).write("");
    }

    @Test
    public void testGetCurrentUser() {

        HttpRequest request = mock(HttpRequest.class);
        assertNull(new TestRequestHandler().getCurrentUser(request));
    }
}

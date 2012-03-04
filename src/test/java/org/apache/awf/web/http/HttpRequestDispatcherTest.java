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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.awf.annotation.Asynchronous;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpException;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.HttpRequestDispatcher;
import org.apache.awf.web.http.HttpResponse;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.junit.Test;

/**
 * Test cases for {@link HttpRequestDispatcher}.
 */
public class HttpRequestDispatcherTest {

    private class TestRequestHandler extends RequestHandler {

        @Override
        public void get(HttpRequest request, HttpResponse response) {
            response.write("get");
        }

        @Override
        public void post(HttpRequest request, HttpResponse response) {
            response.write("post");
        }

        @Override
        public void head(HttpRequest request, HttpResponse response) {
            response.write("head");
        }

        @Override
        public void put(HttpRequest request, HttpResponse response) {
            response.write("put");
        }

        @Override
        public void delete(HttpRequest request, HttpResponse response) {
            response.write("delete");
        }
    }

    @Test
    public void testDispatchForGet() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.GET);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).write("get");
    }

    @Test
    public void testDispatchForPost() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.POST);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).write("post");
    }

    @Test
    public void testDispatchForHead() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.HEAD);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).write("head");
    }

    @Test
    public void testDispatchForPut() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.PUT);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).write("put");
    }

    @Test
    public void testDispatchForDelete() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.DELETE);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).write("delete");
    }

    @Test
    public void testDispatchForOptions() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.OPTIONS);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(0)).write(anyString());
    }

    @Test
    public void testDispatchForTrace() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.TRACE);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(0)).write(anyString());
    }

    @Test
    public void testDispatchForConnect() {

        RequestHandler handler = new TestRequestHandler();
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.CONNECT);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(0)).write(anyString());
    }

    @Test
    public void testDispatchForHttpException() {

        RequestHandler handler = new TestRequestHandler() {
            @Override
            @Asynchronous
            public void get(HttpRequest request, HttpResponse response) {
                throw new HttpException(HttpStatus.CLIENT_ERROR_BAD_REQUEST, "testMessage");
            }
        };

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        when(request.getMethod()).thenReturn(HttpVerb.GET);

        HttpRequestDispatcher.dispatch(handler, request, response);
        verify(response, times(1)).setStatus(HttpStatus.CLIENT_ERROR_BAD_REQUEST);
        verify(response, times(1)).write("testMessage");
        verify(response, times(1)).finish();
    }
}

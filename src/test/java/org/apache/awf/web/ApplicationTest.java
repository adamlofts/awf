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
package org.apache.awf.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.apache.awf.web.Application;
import org.apache.awf.web.handler.BadRequestRequestHandler;
import org.apache.awf.web.handler.NotFoundRequestHandler;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.HttpRequestImpl;
import org.apache.awf.web.http.HttpResponse;
import org.junit.Test;

public class ApplicationTest {

    @Test
    public void simpleApplicationTest() {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        final RequestHandler handler1 = new RequestHandler() {
            @Override
            public void get(HttpRequest request, HttpResponse response) {
            }
        };
        final RequestHandler handler2 = new RequestHandler() {
            @Override
            public void get(HttpRequest request, HttpResponse response) {
            }
        };
        final RequestHandler handler3 = new RequestHandler() {
            @Override
            public void get(HttpRequest request, HttpResponse response) {
            }
        };
        final RequestHandler handler4 = new RequestHandler() {
            @Override
            public void get(HttpRequest request, HttpResponse response) {
            }
        };

        handlers.put("/", handler1);
        handlers.put("/persons/([0-9]+)", handler2);
        handlers.put("/persons/phone_numbers", handler3);
        handlers.put("/pets/([0-9]{0,3})", handler4);
        Application app = new Application(handlers);

        String requestLine = "GET / HTTP/1.1";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("host", "localhost");
        HttpRequest request = new HttpRequestImpl(requestLine, headers);

        assertNotNull(app.getHandler(request));

        requestLine = "GET /persons/1911 HTTP/1.1";
        request = new HttpRequestImpl(requestLine, headers);
        assertNotNull(app.getHandler(request));

        requestLine = "GET /persons/phone_numbers HTTP/1.1";
        request = new HttpRequestImpl(requestLine, headers);
        assertNotNull(app.getHandler(request));

        requestLine = "GET /pets/123 HTTP/1.1";
        request = new HttpRequestImpl(requestLine, headers);
        assertNotNull(app.getHandler(request));

        request = new HttpRequestImpl("GET /missing HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /persons HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /persons/roger HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /persons/123a HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /persons/a123 HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /pets/a123 HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /pets/123a HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET /pets/1234 HTTP/1.1", headers);
        assertEquals(NotFoundRequestHandler.getInstance(), app.getHandler(request));

        request = new HttpRequestImpl("GET / HTTP/1.1", headers);
        assertFalse(handler1.equals(app.getHandler(request)));
        assertEquals(handler1.getClass(), app.getHandler(request).getClass());

        request = new HttpRequestImpl("GET /persons/1911 HTTP/1.1", headers);
        assertFalse(handler2.equals(app.getHandler(request)));
        assertEquals(handler2.getClass(), app.getHandler(request).getClass());

        request = new HttpRequestImpl("GET /persons/phone_numbers HTTP/1.1", headers);
        assertFalse(handler3.equals(app.getHandler(request)));
        assertEquals(handler3.getClass(), app.getHandler(request).getClass());

        request = new HttpRequestImpl("GET /pets/123 HTTP/1.1", headers);
        assertFalse(handler4.equals(app.getHandler(request)));
        assertEquals(handler4.getClass(), app.getHandler(request).getClass());

        // Verify that BadRequestRequestHandler is returned if request does not
        // include Host header
        headers = new HashMap<String, String>();
        request = new HttpRequestImpl("GET /pets/123 HTTP/1.1", headers);
        assertEquals(BadRequestRequestHandler.getInstance(), app.getHandler(request));

    }

    @Test(expected = PatternSyntaxException.class)
    public void malFormedRegularExpressionTest() {
        Map<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
        final RequestHandler handler1 = new RequestHandler() {
            @Override
            public void get(HttpRequest request, HttpResponse response) {
            }
        };

        handlers.put("/persons/([[0-9]{0,3})", handler1);
        new Application(handlers);
    }
}

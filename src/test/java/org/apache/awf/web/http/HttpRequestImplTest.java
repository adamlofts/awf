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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class HttpRequestImplTest {

    @Test
    public void testHttpRequestNoQueryString() {
        String requestLine = "GET /foobar HTTP/1.1 ";
        HttpRequest request = new HttpRequestImpl(requestLine, new HashMap<String, String> ());
        Assert.assertEquals("/foobar", request.getRequestedPath());
    }

    @Test
    public void testHttpRequestNullQueryString() {
        String requestLine = "GET /foobar? HTTP/1.1 ";
        HttpRequest request = new HttpRequestImpl(requestLine, new HashMap<String, String>());
        Assert.assertEquals("/foobar", request.getRequestedPath());
    }

    @Test
    public void testHttpRequestNullQueryStringTrailingSlash() {
        String requestLine = "GET /foobar/? HTTP/1.1 ";
        HttpRequest request = new HttpRequestImpl(requestLine, new HashMap<String, String>());
        Assert.assertEquals("/foobar/", request.getRequestedPath());
    }

    @Test
    public void doNotExpectContinueWhenBodyNotEmptyWithExpectHeader(){
        HttpRequestImpl request = new HttpRequestImpl();
        request.pushToHeaders("content-length", "12");
        request.pushToHeaders("expect", "niniin");
        request.getContentLength();
        request.getBodyBuffer().put("12345".getBytes());
        Assert.assertFalse("Expect continue should be false when body is submitted", request.expectContinue());

    }

    @Test
    public void expectContinueWhenBodyNotNullButEmptyWithExpectHeader(){
        HttpRequestImpl request = new HttpRequestImpl();
        request.pushToHeaders("content-length", "12");
        request.pushToHeaders("expect", "niniin");
        request.getContentLength();
        Assert.assertTrue("Expect continue should be false when body bot submitted", request.expectContinue());
    }

    @Test
    public void expectContinueWhenBodyNullWithExpectHeader(){
        HttpRequestImpl request = new HttpRequestImpl();
        request.pushToHeaders("expect", "niniin");
        request.getContentLength();
        Assert.assertTrue("Expect continue should be false when body is submitted", request.expectContinue());
    }
}

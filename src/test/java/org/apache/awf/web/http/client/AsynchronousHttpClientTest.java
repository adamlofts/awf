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
package org.apache.awf.web.http.client;

import static org.apache.awf.web.http.client.AsynchronousHttpClient.HTTP_VERSION;
import static org.apache.awf.web.http.client.AsynchronousHttpClient.NEWLINE;
import static org.apache.awf.web.http.client.AsynchronousHttpClient.USER_AGENT_HEADER;
import static org.junit.Assert.assertEquals;

import org.apache.awf.web.AsyncResult;
import org.apache.awf.web.http.client.AsynchronousHttpClient;
import org.apache.awf.web.http.client.Response;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.junit.Test;

/**
 * Test cases for {@link AsynchronousHttpClient}.
 */
public class AsynchronousHttpClientTest {

    @Test
    public void testMakeRequestLineAndHeaders() {

        AsynchronousHttpClient client = new AsynchronousHttpClient() {
            @Override
            protected void doFetch(AsyncResult<Response> callback, long requestStarted) {
                // Do nothing.
            }
        };

        client.get("http://testurl.com/path/", null);

        String expected = HttpVerb.GET + " /path/ " + HTTP_VERSION;
        expected += "Host: testurl.com" + NEWLINE + USER_AGENT_HEADER;
        expected += NEWLINE;

        String actual = client.makeRequestLineAndHeaders();

        assertEquals(expected, actual);
    }

    @Test
    public void testMakeRequestLineAndHeadersWithBody() {

        AsynchronousHttpClient client = new AsynchronousHttpClient() {
            @Override
            protected void doFetch(AsyncResult<Response> callback, long requestStarted) {
                // Do nothing.
            }
        };

        client.post("http://testurl.com/path/", "name=value", null);

        String expected = HttpVerb.POST + " /path/ " + HTTP_VERSION;
        expected += "Host: testurl.com" + NEWLINE + USER_AGENT_HEADER;
        expected += "Content-Type: application/x-www-form-urlencoded" + NEWLINE;
        expected += "Content-Length: 10";
        expected += NEWLINE + NEWLINE;
        expected += "name=value";
        expected += NEWLINE;

        String actual = client.makeRequestLineAndHeaders();

        assertEquals(expected, actual);
    }

    @Test
    public void testMakeRequestLineAndHeadersWithZeroLengthBody() {

        AsynchronousHttpClient client = new AsynchronousHttpClient() {
            @Override
            protected void doFetch(AsyncResult<Response> callback, long requestStarted) {
                // Do nothing.
            }
        };

        client.post("http://testurl.com/path/", "", null);

        String expected = HttpVerb.POST + " /path/ " + HTTP_VERSION;
        expected += "Host: testurl.com" + NEWLINE + USER_AGENT_HEADER;
        expected += "Content-Type: application/x-www-form-urlencoded" + NEWLINE;
        expected += "Content-Length: 0";
        expected += NEWLINE + NEWLINE;
        expected += NEWLINE;

        String actual = client.makeRequestLineAndHeaders();

        assertEquals(expected, actual);
    }
}

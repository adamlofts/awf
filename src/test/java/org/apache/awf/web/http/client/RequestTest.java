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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.awf.web.http.client.Request;
import org.apache.awf.web.http.protocol.ContentType;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.junit.Test;

import com.google.common.base.Charsets;

/**
 * Test cases for {@link Request}.
 */
public class RequestTest {

    @Test
    public void testRequest() {

        final Request request = new Request("http://testurl.com:8080", HttpVerb.POST, false, 99);

        assertEquals(request.getVerb(), HttpVerb.POST);

        assertEquals("http", request.getURL().getProtocol());
        assertEquals("testurl.com", request.getURL().getHost());
        assertEquals(8080, request.getURL().getPort());

        assertEquals(false, request.isFollowingRedirects());
        assertEquals(99, request.getMaxRedirects());
    }

    @Test(expected = RuntimeException.class)
    public void testRequestForMalformedUrl() {

        new Request("malformed", HttpVerb.POST);
    }

    @Test
    public void testSetGetBody() {

        final Request request = new Request("http://unimportant-value.com", HttpVerb.POST);

        request.setBody("testContent1");
        assertTrue(Arrays.equals("testContent1".getBytes(), request.getBody()));

        request.setBody("testContent2".getBytes());
        assertEquals("testContent2", new String(request.getBody(), Charsets.ISO_8859_1));
    }

    @Test
    public void testSetGetContentType() {

        final Request request = new Request("http://unimportant-value.com", HttpVerb.POST);
        assertEquals(ContentType.APPLICATION_FORM_URLENCODED, request.getContentType());

        request.setContentType(ContentType.MULTIPART_FORM_DATA);
        assertEquals(ContentType.MULTIPART_FORM_DATA, request.getContentType());
    }
}

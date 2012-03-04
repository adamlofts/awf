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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.awf.util.HttpRequestHelper;
import org.apache.awf.util.HttpUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRequestParserTest {

    
    private HttpRequestParser parser;

    @Before
    public void init(){
        parser = new HttpRequestParser();
    }

    @Test
    public void testDeserializeHttpGetRequest() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addHeader("Host", "127.0.0.1:8080");
        helper.addHeader("User-Agent", "curl/7.19.5 (i386-apple-darwin10.0.0) libcurl/7.19.5 zlib/1.2.3");
        helper.addHeader("Accept", "*/*");
        ByteBuffer bb1 = helper.getRequestAsByteBuffer();

        helper = new HttpRequestHelper();
        helper.addHeader("Host", "127.0.0.1:8080");
        helper.addHeader("User-Agent",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; sv-SE; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        helper.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        helper.addHeader("Accept-Language", "sv-se,sv;q=0.8,en-us;q=0.5,en;q=0.3");
        helper.addHeader("Accept-Encoding", "gzip,deflate");
        helper.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        helper.addHeader("Keep-Alive", "115");
        helper.addHeader("Connection", "keep-alve");
        ByteBuffer bb2 = helper.getRequestAsByteBuffer();

        HttpRequest request1 = parser.parseRequestBuffer(bb1);
        HttpRequest request2 = parser.parseRequestBuffer(bb2);

        assertEquals("GET / HTTP/1.1", request1.getRequestLine());
        assertEquals("GET / HTTP/1.1", request2.getRequestLine());

        assertEquals(4, request1.getHeaders().size());
        assertEquals(9, request2.getHeaders().size());

        List<String> expectedHeaderNamesInRequest1 = Arrays.asList(new String[] { "User-Agent", "Host", "Accept",
                "From" });
        for (String expectedHeaderName : expectedHeaderNamesInRequest1) {
            assertTrue(request1.getHeaders().containsKey(expectedHeaderName.toLowerCase()));
        }

        List<String> expectedHeaderNamesInRequest2 = Arrays.asList(new String[] { "Host", "User-Agent", "Accept",
                "From", "Accept-Language", "Accept-Encoding", "Accept-Charset", "Keep-Alive", "Connection" });
        for (String expectedHeaderName : expectedHeaderNamesInRequest2) {
            assertTrue(request2.getHeaders().containsKey(expectedHeaderName.toLowerCase()));
        }

        // TODO RS 100920 verify that the headers exist
    }

    @Test
    public void shouldSupportChunkedEncodingOnCompleteRequest(){
        String post= "POST /path/script.cgi HTTP/1.0\r\n"
                + "Host: localhost\r\n"
                + "From: frog@jmarshall.com\r\n"
                + "User-Agent: HTTPTool/1.0\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"+
                "Transfer-Encoding: chunked\r\n\r\n" +
                "1a ; ignore-stuff-here\r\n" +
                "abcdefghijklmnopqrstuvwxyz\r\n" +
                "10 \r\n" +
                "1234567890abcdef\r\n" +
                "0\r\n" +
                "some-footer: some-value\r\n" +
                "another-footer: another-value\r\n\r\n";
        HttpRequestImpl request = parser.parseRequestBuffer(ByteBuffer.wrap(post.getBytes()));
        
        assertEquals("abcdefghijklmnopqrstuvwxyz1234567890abcdef", request.getBody());
        assertEquals("Should contain footer some-footer", "some-value", request.getHeader("some-footer"));
        assertEquals("Should contain footer another-footer", "another-value", request.getHeader("another-footer"));
    }


    @Test
    public void shouldSupportChunkedEncodingOnPartialRequest(){
        String postFirst= "POST /path/script.cgi HTTP/1.0\r\n"
        + "Host: localhost\r\n"
        + "From: frog@jmarshall.com\r\n"
        + "User-Agent: HTTPTool/1.0\r\n"
        + "Content-Type: application/x-www-form-urlencoded\r\n"+
        "Transfer-Encoding: chunked\r\n\r\n" +
        "1a; ignore-stuff-here\r\n";
        String postBis = "abcdefghijklmnopqrstuvwxyz\r\n" +
        "10\r\n";
        String postTer = "12345678";
        String postQuart="90abcdef\r\n" +
                            "0\r\n" +
                            "some-footer: some-value\r\n" +
                            "another-footer: another-value\r\n\r\n";
        HttpRequestImpl request = parser.parseRequestBuffer(ByteBuffer.wrap(postFirst.getBytes()));
        assertFalse("Request should not be finished on incomplete", request.isFinished());
        request = parser.parseRequestBuffer(ByteBuffer.wrap(postBis.getBytes()), request);

        assertFalse("Request should not be finished on incomplete", request.isFinished());

        request = parser.parseRequestBuffer(ByteBuffer.wrap(postTer.getBytes()), request);
        assertFalse("Request should not be finished on incomplete", request.isFinished());

        request = parser.parseRequestBuffer(ByteBuffer.wrap(postQuart.getBytes()), request);
        assertTrue("Request should be finished on complete", request.isFinished());

        assertEquals("abcdefghijklmnopqrstuvwxyz1234567890abcdef", request.getBody());
        assertEquals("Should contain footer some-footer", "some-value", request.getHeader("some-footer"));
        assertEquals("Should contain footer another-footer", "another-value", request.getHeader("another-footer"));

    }




    @Test
    public void testSingleGetParameter() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("firstname", "jim");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

        assertEquals(1, request.getParameters().size());
        assertEquals("jim", request.getParameter("firstname"));
    }

    @Test
    public void testMultipleGetParameter() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("firstname", "jim");
        helper.addGetParameter("lastname", "petersson");
        helper.addGetParameter("city", "stockholm");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(3, getSize(params));
        assertEquals("jim", request.getParameter("firstname"));
        assertEquals("petersson", request.getParameter("lastname"));
        assertEquals("stockholm", request.getParameter("city"));
    }

    private int getSize(Map<String, Collection<String>> mmap) {
        int size = 0;
        for (Collection<String> values : mmap.values()) {
            size += values.size();
        }
        return size;
    }

    @Test
    public void testSingleParameterWithoutValue() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("firstname", null);

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();
        assertEquals(0, getSize(params));
        assertEquals(null, request.getParameter("firstname"));
    }

    @Test
    public void testMultipleParametersWithoutValue() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("firstname", null);
        helper.addGetParameter("lastName", "");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(0, getSize(params));
        assertEquals(null, request.getParameter("firstname"));
        assertEquals(null, request.getParameter("lastName"));
    }
    
    @Test
    public void testEncodedParametersValue() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("email", "name%40server");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(1, getSize(params));
        assertEquals("name@server", request.getParameter("email"));
    }

    @Test
    public void testMultipleParametersWithAndWithoutValue() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("firstname", null);
        helper.addGetParameter("lastName", "petersson");
        helper.addGetParameter("city", "");
        helper.addGetParameter("phoneno", "12345");
        helper.addGetParameter("age", "30");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(3, getSize(params));
        assertEquals(null, request.getParameter("firstname"));
        assertEquals("petersson", request.getParameter("lastName"));
        assertEquals(null, request.getParameter("city"));
        assertEquals("12345", request.getParameter("phoneno"));
        assertEquals("30", request.getParameter("age"));
    }

    @Test
    public void testSingleGetParameterMultipleValues() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("letters", "x");
        helper.addGetParameter("letters", "y");
        helper.addGetParameter("letters", "z");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(3, getSize(params));
        Collection<String> values = params.get("letters");
        assertEquals(3, values.size());
        assertTrue(values.contains("x"));
        assertTrue(values.contains("y"));
        assertTrue(values.contains("z"));
    }

    @Test
    public void testMultipleGetParametersMultipleValues() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("letters", "x");
        helper.addGetParameter("letters", "y");
        helper.addGetParameter("letters", "z");
        helper.addGetParameter("numbers", "23");
        helper.addGetParameter("numbers", "54");
        helper.addGetParameter("country", "swe");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(6, getSize(params));
        Collection<String> letters = params.get("letters");
        Collection<String> numbers = params.get("numbers");
        Collection<String> country = params.get("country");

        assertEquals(3, letters.size());
        assertEquals(2, numbers.size());
        assertEquals(1, country.size());

        assertTrue(letters.contains("x"));
        assertTrue(letters.contains("y"));
        assertTrue(letters.contains("z"));

        assertTrue(numbers.contains("23"));
        assertTrue(numbers.contains("54"));

        assertTrue(country.contains("swe"));
    }

    @Test
    public void testSingleGetParameterMultipleValuesIncludingNull() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("letters", "x");
        helper.addGetParameter("letters", "y");
        helper.addGetParameter("letters", null);
        helper.addGetParameter("letters", "z");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();

        assertEquals(3, getSize(params));
        Collection<String> values = params.get("letters");
        assertEquals(3, values.size());
        assertTrue(values.contains("x"));
        assertTrue(values.contains("y"));
        assertTrue(values.contains("z"));
    }

    @Test
    public void testEmptyParameters() {
        HttpRequestHelper helper = new HttpRequestHelper();
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();
        assertNotNull(params);
        assertEquals(0, getSize(params));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableParameters() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addGetParameter("letter", "x");

        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        Map<String, Collection<String>> params = request.getParameters();
        params.put("not", new ArrayList<String>());
    }

    @Test
    public void testHostVerification_exists_HTTP_1_0() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.setVersion("1.0");
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        boolean requestOk = HttpUtil.verifyRequest(request);
        assertTrue(requestOk);
    }

    @Test
    public void testHostVerification_nonExisting_HTTP_1_0() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.setVersion("1.0");
        helper.removeHeader("Host");
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        boolean requestOk = HttpUtil.verifyRequest(request);
        assertTrue(requestOk);
    }

    @Test
    public void testHostVerification_exists_HTTP_1_1() {
        HttpRequestHelper helper = new HttpRequestHelper();
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        boolean requestOk = HttpUtil.verifyRequest(request);
        assertTrue(requestOk);
    }

    @Test
    public void testHostVerification_nonExisting_HTTP_1_1() {
        HttpRequestHelper helper = new HttpRequestHelper();
        helper.removeHeader("Host");
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
        boolean requestOk = HttpUtil.verifyRequest(request);
        assertFalse(requestOk);
    }

    @Test
    public void testGarbageRequest() {
        HttpRequest request =  parser.parseRequestBuffer(ByteBuffer.wrap(new byte[] { 1, 1, 1, 1 } // garbage
                ));
    }

    /**
     * Ensure that header keys are converted to lower case, to facilitate
     * case-insensitive retrieval through
     * {@link org.apache.awf.web.http.HttpRequestImpl#getHeader(String)}.
     */
    @Test
    public void testOfConvertsHeaderKeysToLowerCase() {

        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addHeader("TESTKEY", "unimportant");
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

        assertFalse(request.getHeaders().containsKey("TESTKEY"));
        assertTrue(request.getHeaders().containsKey("testkey"));
    }

    /**
     * Ensure that the case of any header values is correctly maintained.
     */
    @Test
    public void testOfMaintainsHeaderValueCase() {

        String expected = "vAlUe";

        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addHeader("TESTKEY", expected);
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

        String actual = request.getHeader("TESTKEY");
        assertEquals(expected, actual);
    }

    /**
     * Ensure that case for any key passed to the method is unimportant for its
     * retrieval.
     */
    @Test
    public void testGetHeader() {

        String expected = "value";

        HttpRequestHelper helper = new HttpRequestHelper();
        helper.addHeader("TESTKEY", expected);
        HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

        assertEquals(expected, request.getHeader("TESTKEY"));
        assertEquals(expected, request.getHeader("testkey"));
    }

    @Test
    public void testHttpRequestNoQueryString() {
        String requestLine = "GET /foobar HTTP/1.1 ";
        HttpRequest request = new HttpRequestImpl(requestLine, new HashMap<String, String>());
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
    public void testNoCookies() {
        HttpRequestHelper hrh = new HttpRequestHelper();
        HttpRequest hr = parser.parseRequestBuffer(hrh.getRequestAsByteBuffer());
        Assert.assertEquals(0, hr.getCookies().size());
    }

    @Test
    public void testOneCookie() {
        HttpRequestHelper hrh = new HttpRequestHelper();
        hrh.addHeader("Cookie", "one=value");
        HttpRequest hr = parser.parseRequestBuffer(hrh.getRequestAsByteBuffer());
        Assert.assertEquals("value", hr.getCookie("one"));
    }

    @Test
    public void testOneCookieWithoutValue() {
        HttpRequestHelper hrh = new HttpRequestHelper();
        hrh.addHeader("Cookie", "one=");
        HttpRequest hr = parser.parseRequestBuffer(hrh.getRequestAsByteBuffer());
        Assert.assertEquals("", hr.getCookie("one"));
    }

    @Test
    public void testMultipleCookies() {
        HttpRequestHelper hrh = new HttpRequestHelper();
        hrh.addHeader("Cookie", "one=value;two=value2");
        HttpRequest hr = parser.parseRequestBuffer(hrh.getRequestAsByteBuffer());
        Assert.assertEquals("value", hr.getCookie("one"));
        Assert.assertEquals("value2", hr.getCookie("two"));
    }

}

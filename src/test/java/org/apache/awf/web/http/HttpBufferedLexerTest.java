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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.apache.awf.web.http.HttpBufferedLexer.*;
import static org.apache.awf.web.http.HttpParsingContext.TokenType;

/**
 * Unit tests suite for HttpBufferedLexer
 */
public class HttpBufferedLexerTest {

    HttpBufferedLexer  lexer;
    HttpParsingContext context;

    @Before
    public void init() {
        lexer = new HttpBufferedLexer();
        context = new HttpParsingContext();
    }

    @Test
    public void testNextTokenSimpleGet() throws Exception {
        String request = "GET /path/script.cgi HTTP/1.1\r\n\r\n";

        context.setBuffer(ByteBuffer.wrap(request.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals("Token GET should be found with no errors", 1, res);
        Assert.assertEquals("GET", context.getTokenValue());
        Assert.assertEquals(TokenType.REQUEST_METHOD, context.currentType);

        res = lexer.nextToken(context);
        Assert.assertEquals("Token uri should be found with no errors", 1, res);
        Assert.assertEquals("/path/script.cgi", context.getTokenValue());
        Assert.assertEquals(TokenType.REQUEST_URI, context.currentType);

        res = lexer.nextToken(context);
        Assert.assertEquals("Token protocol version should be found with no errors", 1, res);
        Assert.assertEquals("HTTP/1.1", context.getTokenValue());
        Assert.assertEquals(TokenType.HTTP_VERSION, context.currentType);

        res = lexer.nextToken(context);
        Assert.assertEquals("Token body should be found with no errors", 1, res);
        Assert.assertEquals(TokenType.BODY, context.currentType);
        Assert.assertEquals("TokenValue should be null for body", "", context.getTokenValue());

    }

    @Test
    public void requestLineContainingCRLF() {
        String request = "GET\r\n /path/script.cgi HTTP/1.1\r\n\r\n";

        context.setBuffer(ByteBuffer.wrap(request.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals("Token GET should be found with error", -1, res);
    }



    @Test
    public void requestWithHeadersParsing() {

        String request = "POST /path/script.cgi HTTP/1.0\r\n"
                + "Host: localhost\r\n"
                + "From: frog@jmarshall.com\r\n"
                + "User-Agent: HTTPTool/1.0\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 32\r\n\r\n";

        context.setBuffer(ByteBuffer.wrap(request.getBytes()));
        String[][] headers = new String[][]{{"Host", " localhost"},
                {"From", " frog@jmarshall.com"},
                {"User-Agent", " HTTPTool/1.0"},
                {"Content-Type", " application/x-www-form-urlencoded"},
                {"Content-Length", " 32"}};

        int res = lexer.nextToken(context);
        String method = context.getTokenValue();
        res = lexer.nextToken(context);
        String path = context.getTokenValue();
        res = lexer.nextToken(context);
        String protocol = context.getTokenValue();

        Assert.assertEquals("POST", method);
        Assert.assertEquals("/path/script.cgi", path);
        Assert.assertEquals("HTTP/1.0", protocol);

        for (String[] header : headers) {
            res = lexer.nextToken(context);
            Assert.assertEquals(1, res);
            Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
            Assert.assertEquals(header[0], context.getTokenValue());
            res = lexer.nextToken(context);
            Assert.assertEquals(1, res);
            Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
            Assert.assertEquals(header[1], context.getTokenValue());
        }

        res = lexer.nextToken(context);
        Assert.assertEquals("Token body should be found with no errors", 1, res);
        Assert.assertEquals(TokenType.BODY, context.currentType);
        Assert.assertEquals("TokenValue should be null for body", "", context.getTokenValue());

    }


    @Test
    public void shouldReadCompleteChunkOctet(){
        String chunk = "\r\n10 ; // ignore this\r\n";
        context.setBuffer(ByteBuffer.wrap(chunk.getBytes()));
        context.currentType = TokenType.CHUNK_OCTET;
        context.chunked = true;
        int res = lexer.nextToken(context);
        Assert.assertTrue("Result should be complete", res == 1);
        Assert.assertEquals("TokenType should be ChunkOctet",TokenType.CHUNK_OCTET,context.currentType);
        Assert.assertEquals("Token should be same as given", "10 ; // ignore this", context.getTokenValue());
    }

    @Test
    public void shouldReadIncompleteChunkOctet(){
        String chunk = "\r\n10 ; // i";
        context.setBuffer(ByteBuffer.wrap(chunk.getBytes()));
        context.currentType = TokenType.CHUNK_OCTET;
        context.chunked = true;
        int res = lexer.nextToken(context);
        Assert.assertTrue("Result should be incomplete", res == 0);
        Assert.assertEquals("TokenType should be ChunkOctet",TokenType.CHUNK_OCTET,context.currentType);
        Assert.assertEquals("Token should be same as given", "10 ; // i", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap("gnore this\r\n".getBytes()));
        res = lexer.nextToken(context);
        Assert.assertTrue("Result should be complete", res == 1);
        Assert.assertEquals("TokenType should be ChunkOctet",TokenType.CHUNK_OCTET,context.currentType);
        Assert.assertEquals("Token should be same as given", "10 ; // ignore this", context.getTokenValue());
    }


    @Test
    public void requestLineWithTrailingHeaders() {
        String request = " \r\n \r\nPOST ";
        context.setBuffer(ByteBuffer.wrap(request.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.REQUEST_METHOD, context.currentType);
        Assert.assertEquals("POST", context.getTokenValue());
    }

    @Test
    public void incompleteHeaderAfterRequestLine() {
        String requestPart1 = "Content-";
        String requestPart2 = "Type: application";
        String requestPart3 = "/x-www-form-";
        String requestPart4 = "urlencoded\r\n";
        context.currentType = TokenType.HTTP_VERSION;
        context.setBuffer(ByteBuffer.wrap(requestPart1.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HTTP_VERSION, context.currentType);
        Assert.assertEquals("Content-", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart2.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals("Content-Type", context.getTokenValue());

        res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(" application", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart3.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(" application/x-www-form-", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart4.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals(" application/x-www-form-urlencoded", context.getTokenValue());
    }

    @Test
    public void incompleteHeaderAfterHeader() {
        String requestPart1 = "Content-";
        String requestPart2 = "Type: application";
        String requestPart3 = "/x-www-form-";
        String requestPart4 = "urlencoded\r\n";
        context.currentType = TokenType.HEADER_VALUE;
        context.setBuffer(ByteBuffer.wrap(requestPart1.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals("Content-", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart2.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals("Content-Type", context.getTokenValue());

        res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(" application", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart3.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(0, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(" application/x-www-form-", context.getTokenValue());

        context.setBuffer(ByteBuffer.wrap(requestPart4.getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals(" application/x-www-form-urlencoded", context.getTokenValue());
    }

    @Test
    public void parseMultiLineHeaders() {
        String request = "my headervalue\r\n and so on\r\n\tand so on\r\n";
        context.currentType = TokenType.HEADER_NAME;
        context.setBuffer(ByteBuffer.wrap(request.getBytes()));

        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals("my headervalue", context.getTokenValue());

        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals("and so on", context.getTokenValue());

        res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals("and so on", context.getTokenValue());
    }

    @Test
    public void failOnMethodAboveMaxSize() {
        StringBuilder request = getRandomString(METHOD_LENGTH + 1).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_LINE, context.currentType);

        request = getRandomString(METHOD_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_LINE, context.currentType);

    }


    @Test
    public void successOnMethodAtMaxSize() {
        StringBuilder request = getRandomString(METHOD_LENGTH).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.REQUEST_METHOD, context.currentType);
        Assert.assertEquals(request.deleteCharAt(METHOD_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void successOnUriAtMaxSize() {
        StringBuilder request = getRandomString(URI_LENGTH).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.REQUEST_METHOD;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.REQUEST_URI, context.currentType);
        Assert.assertEquals(request.deleteCharAt(URI_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void failOnUriAboveMaxSize() {
        StringBuilder request = getRandomString(URI_LENGTH + 1).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.REQUEST_METHOD;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_METHOD, context.currentType);

        request = getRandomString(URI_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_METHOD, context.currentType);
    }

    @Test
    public void successOnVersionAtMaxSize() {
        StringBuilder request = getRandomString(VERSION_LENGTH).append('\r');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.REQUEST_URI;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HTTP_VERSION, context.currentType);
        Assert.assertEquals(request.deleteCharAt(VERSION_LENGTH).toString(),
                context.getTokenValue());
    }

    @Test
    public void failOnVersionAboveMaxSize() {
        StringBuilder request = getRandomString(VERSION_LENGTH + 1).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.REQUEST_URI;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_URI, context.currentType);

        request = getRandomString(VERSION_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.REQUEST_URI, context.currentType);
    }

    @Test
    public void successOnHeaderNameAtMaxSizeFromRequestLine() {
        StringBuilder request = getRandomString(HEADER_NAME_LENGTH).append(':');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HTTP_VERSION;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(request.deleteCharAt(HEADER_NAME_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void failOnHeaderNameAboveMaxSizeFromRequestLine() {
        StringBuilder request = getRandomString(HEADER_NAME_LENGTH + 1).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HTTP_VERSION;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HTTP_VERSION, context.currentType);

        request = getRandomString(HEADER_NAME_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HTTP_VERSION, context.currentType);
    }

    @Test
    public void successOnHeaderNameAtMaxSizeFromHeaderValue() {
        StringBuilder request = getRandomString(HEADER_NAME_LENGTH).append(':');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_VALUE;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
        Assert.assertEquals(request.deleteCharAt(HEADER_NAME_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void failOnHeaderNameAboveMaxSizeFromHeaderValue() {
        StringBuilder request = getRandomString(HEADER_NAME_LENGTH + 1).append(' ');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_VALUE;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);

        request = getRandomString(HEADER_NAME_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
    }

    @Test
    public void successOnHeaderValueAtMaxSize() {
        StringBuilder request = getRandomString(HEADER_VALUE_LENGTH).append('\r');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_NAME;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals(request.deleteCharAt(HEADER_VALUE_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void failOnHeaderValueAboveMaxSize() {
        StringBuilder request = getRandomString(HEADER_VALUE_LENGTH + 1).append('\r');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_NAME;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);

        request = getRandomString(HEADER_NAME_LENGTH + 1);
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_NAME, context.currentType);
    }

    @Test
    public void successOnHeaderValueAtMaxSizeOnMultiLine() {
        StringBuilder request = getRandomString(HEADER_VALUE_LENGTH).insert(0, '\t').append('\r');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_VALUE;
        int res = lexer.nextToken(context);
        Assert.assertEquals(1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
        Assert.assertEquals(request.deleteCharAt(0).deleteCharAt(HEADER_VALUE_LENGTH).toString(), context.getTokenValue());
    }

    @Test
    public void failOnHeaderValueAboveMaxSizeOnMultiLine() {
        StringBuilder request = getRandomString(HEADER_VALUE_LENGTH + 1).insert(0, ' ').append('\r');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        context.currentType = TokenType.HEADER_VALUE;
        int res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);

        request = getRandomString(HEADER_VALUE_LENGTH + 1).insert(0, '\t');
        context.setBuffer(ByteBuffer.wrap(request.toString().getBytes()));
        res = lexer.nextToken(context);
        Assert.assertEquals(-1, res);
        Assert.assertEquals(TokenType.HEADER_VALUE, context.currentType);
    }

    /**
     * Generate
     *
     * @param length
     * @return
     */
    private StringBuilder getRandomString(int length) {

        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append('A');
        }
        return stringBuilder;
    }

}

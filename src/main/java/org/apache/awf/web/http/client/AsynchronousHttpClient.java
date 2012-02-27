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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

import org.apache.awf.io.AsynchronousSocket;
import org.apache.awf.io.IOLoop;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.util.NopAsyncResult;
import org.apache.awf.util.UrlUtil;
import org.apache.awf.web.AsyncCallback;
import org.apache.awf.web.AsyncResult;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.apache.awf.web.http.client.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * This class implements a simple HTTP 1.1 client on top of the AWF {@code
 * AsynchronousSocket}. It does not currently implement all applicable parts of
 * the HTTP specification.
 * 
 * <pre>
 * E.g the following is not supported.
 *  - keep-alive
 * 
 * </pre>
 * 
 * This class has not been tested extensively in production and should be
 * considered experimental. This HTTP client is
 * inspired by
 * https://github.com/facebook/tornado/blob/master/tornado/simple_httpclient.py
 * and part of the documentation is simply copy pasted.
 */
public class AsynchronousHttpClient {

    /** The <code>Logger</code>. */
    private static final Logger logger = LoggerFactory.getLogger(AsynchronousHttpClient.class);

    private static final long TIMEOUT = 15 * 1000; // 15s

    private static final AsyncResult<Response> nopAsyncResult = NopAsyncResult.of(Response.class).nopAsyncResult;

    private AsynchronousSocket socket;

    private Request request;
    private long requestStarted;
    private Response response;
    private AsyncResult<Response> responseCallback;

    private Timeout timeout;

    private final IOLoop ioLoop;

    static final String HTTP_VERSION = "HTTP/1.1\r\n";
    static final String USER_AGENT_HEADER = "User-Agent: AWF AsynchronousHttpClient/0.2-SNAPSHOT\r\n";
    static final String NEWLINE = "\r\n";

    private final SocketPool pool;

    /**
     * Create an instance of this type.
     */
    public AsynchronousHttpClient() {
        this(IOLoop.INSTANCE, SocketPool.getDefault());
    }

    /**
     * Create an instance of this type, utilising the given <code>IOLoop</code>.
     * 
     * @param ioLoop the <code>IOLoop</code> to use.
     * @param pool the <code>SocketPool</code> to use.
     */
    public AsynchronousHttpClient(final IOLoop ioLoop, final SocketPool pool) {
        this.ioLoop = ioLoop;
        this.pool = pool;
    }

    /**
     * Make an asynchronous call as per the given <code>Request</code>, invoking
     * the passed callback upon completion.
     * 
     * @param request the definition of the request to make.
     * @param callback the callback to execute when the response is received.
     * @throws IllegalArgumentException where the <code>URL</code> associated
     *             with the given <code>Request</code> holds a <code>null</code>
     *             host or invalid port number.
     */
    public void fetch(final Request request, final AsyncResult<Response> callback) {
        this.request = request;
        doFetch(callback, System.currentTimeMillis());
    }

    /**
     * Make an asynchronous HTTP GET request against the specified URL, and
     * invoke the given callback when the response upon completion.
     * 
     * @param url the URL from which to request, e.g.
     *            <em>http://incubator.apache.org/awf/</em>.
     * @param callback the callback to execute when the response is received.
     * @throws IllegalArgumentException where the given URL is parsed to a
     *             <code>null</code> host or with an invalid port number.
     */
    public void get(final String url, final AsyncResult<Response> callback) {
        request = new Request(url, HttpVerb.GET);
        doFetch(callback, System.currentTimeMillis());
    }

    /**
     * Make an asynchronous HTTP POST request against the specified URL, and
     * invoke the given callback when the response upon completion.
     * 
     * @param url the URL from which to request, e.g.
         *            <em>http://incubator.apache.org/awf/</em>.
     * @param body the message body to pass.
     * @param callback the callback to execute when the response is received.
     * @throws IllegalArgumentException where the given URL is parsed to a
     *             <code>null</code> host or with an invalid port number.
     */
    public void post(final String url, final String body, final AsyncResult<Response> callback) {
        request = new Request(url, HttpVerb.POST);
        request.setBody(body);
        doFetch(callback, System.currentTimeMillis());
    }

    /**
     * Make an asynchronous HTTP POST request against the specified URL, and
     * invoke the given callback when the response upon completion.
     * 
     * @param url the URL from which to request, e.g.
     *            <em>http://incubator.apache.org/awf/</em>.
     * @param body the message body to pass.
     * @param callback the callback to execute when the response is received.
     * @throws IllegalArgumentException where the given URL is parsed to a
     *             <code>null</code> host or with an invalid port number.
     */
    public void post(final String url, final byte[] body, final AsyncResult<Response> callback) {
        request = new Request(url, HttpVerb.POST);
        request.setBody(body);
        doFetch(callback, System.currentTimeMillis());
    }

    /**
     * Make an asynchronous HTTP PUT request against the specified URL, and
     * invoke the given callback when the response upon completion.
     * 
     * @param url the URL from which to request, e.g.
     *            <em>http://incubator.apache.org/awf/</em>.
     * @param body the message body to pass.
     * @param callback the callback to execute when the response is received.
     * @throws IllegalArgumentException where the given URL is parsed to a
     *             <code>null</code> host or with an invalid port number.
     */
    public void put(final String url, final String body, final AsyncResult<Response> callback) {
        request = new Request(url, HttpVerb.PUT);
        request.setBody(body);
        doFetch(callback, System.currentTimeMillis());
    }

    /**
     * Perform the action of making a request against a known URL, before
     * invoking the given callback type.
     * 
     * @param callback the callback to execute when the response is received.
     * @param requestStarted the current time in milliseconds at which the
     *            request was begun.
     */
    protected void doFetch(final AsyncResult<Response> callback, final long requestStarted) {
        final String host = request.getURL().getHost();
        int port = request.getURL().getPort();
        port = port == -1 ? 80 : port;

        this.requestStarted = requestStarted;
        responseCallback = callback;

        // Look in the connection pool for the socket and shortcut if found
        socket = pool.getPooledSocket(host, port);
        if (socket != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found pooled socket for " + host + ":" + port);
            }
            startTimeout();
            onConnect();
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Creating new socket for " + host + ":" + port);
        }
        try {
            socket = new AsynchronousSocket(SocketChannel.open());
        } catch (final IOException e) {
            logger.error("Error opening SocketChannel: {}" + e.getMessage());
        }

        startTimeout();
        socket.connect(request.getURL().getHost(), port, new AsyncResult<Boolean>() {
            public void onFailure(final Throwable t) {
                onConnectFailure(t);
            }

            public void onSuccess(final Boolean result) {
                onConnect();
            }
        });
    }

    /**
     * Close the underlaying {@code AsynchronousSocket}.
     */
    public void close() {
        logger.debug("Closing http client connection...");
        socket.close();
    }

    private void startTimeout() {
        logger.debug("start timeout...");
        timeout = new Timeout(System.currentTimeMillis() + TIMEOUT, new AsyncCallback() {
            public void onCallback() {
                onTimeout();
            }
        });
        ioLoop.addTimeout(timeout);
    }

    private void cancelTimeout() {
        logger.debug("cancel timeout...");
        timeout.cancel();
        timeout = null;
    }

    private void onTimeout() {
        logger.debug("Pending operation (connect, read or write) timed out...");
        final AsyncResult<Response> cb = responseCallback;
        responseCallback = nopAsyncResult;
        cb.onFailure(new TimeoutException("Connection timed out"));
        close();
    }

    private void onConnect() {
        logger.debug("Connected...");
        cancelTimeout();
        startTimeout();
        socket.write(makeRequestLineAndHeaders().getBytes(), new AsyncCallback() {
            public void onCallback() {
                onWriteComplete();
            }
        });
    }

    private void onConnectFailure(final Throwable t) {
        logger.debug("Connect failed...");
        cancelTimeout();
        final AsyncResult<Response> cb = responseCallback;
        responseCallback = nopAsyncResult;
        cb.onFailure(t);
        close();
    }

    /**
     * Create the request lines and header, populating the body where not
     * <code>null</code>. Ignoring origin of inputs, the output for a POST
     * request might resemble:
     * <p>
     * <code>
     * POST /path/to/target HTTP/1.1<br/>
     * Host: hostname<br/>
     * User-Agent: AWF AsynchronousHttpClient/0.x-SNAPSHOT<br/>
     * Content-Type: application/x-www-form-urlencoded<br/>
     * Content-Length: 20<br/>
     * <br/>
     * paramName=paramValue<br/>
	 * </code>
     * </p>
     * 
     * @see HttpVerb#hasRequestBody(HttpVerb)
     */
    String makeRequestLineAndHeaders() {

        int length = request.getBody() == null ? 0 : request.getBody().length;
        final StringBuilder builder = new StringBuilder(length + 1024);
        builder.append(request.getVerb()).append(" ").append(request.getURL().getPath()).append(" ");
        builder.append(HTTP_VERSION).append("Host: ").append(request.getURL().getHost()).append(NEWLINE);
        builder.append(USER_AGENT_HEADER);

        if (request.getBody() != null) {
            builder.append("Content-Type: ").append(request.getContentType().toString()).append(NEWLINE);
            builder.append("Content-Length: ").append(length);
            builder.append(NEWLINE).append(NEWLINE);
            builder.append(new String(request.getBody(), Charsets.ISO_8859_1));
        }

        builder.append(NEWLINE);
        return builder.toString();
    }

    private void onWriteComplete() {
        logger.debug("onWriteComplete...");
        cancelTimeout();
        startTimeout();
        socket.readUntil("\r\n\r\n".getBytes(), /* header delimiter */
        new NaiveAsyncResult() {
            public void onSuccess(final byte[] headers) {
                onHeaders(headers);
            }
        });
    }

    private void onHeaders(final byte[] rawResult) {
        String result = new String(rawResult, Charsets.ISO_8859_1);
        logger.debug("headers: {}", result);
        cancelTimeout();
        response = new Response(requestStarted);
        final String[] headers = result.split("\r\n");
        response.setStatuLine(headers[0]); // first entry contains status
        // line
        // (e.g. HTTP/1.1 200 OK)
        for (int i = 1; i < headers.length; i++) {
            final String[] header = headers[i].split(": ");
            response.setHeader(header[0], header[1]);
        }

        final String contentLength = response.getHeader("Content-Length");
        startTimeout();
        if (contentLength != null) {
            socket.readBytes(Integer.parseInt(contentLength), new NaiveAsyncResult() {
                public void onSuccess(byte[] body) {
                    onBody(body);
                }
            });
        } else { // Transfer-Encoding: chunked
            socket.readUntil(NEWLINE.getBytes(), /* chunk delimiter */
            new NaiveAsyncResult() {
                public void onSuccess(byte[] octet) {
                    onChunkOctet(octet);
                }
            });
        }
    }

    /**
     * JM: TODO, especially noting the redirects we follow....
     */
    private void onBody(final byte[] rawBody) {
        final String body = new String(rawBody, Charsets.ISO_8859_1);
        logger.debug("body size: {}", body.length());
        cancelTimeout();
        response.setBody(body);
        if ((response.getStatusLine().contains("301") || response.getStatusLine().contains("302"))
                && request.isFollowingRedirects() && request.getMaxRedirects() > 0) {
            final String newUrl = UrlUtil.urlJoin(request.getURL(), response.getHeader("Location"));
            request = new Request(newUrl, request.getVerb(), true, request.getMaxRedirects() - 1);
            logger.debug("Following redirect, new url: {}, redirects left: {}", newUrl, request.getMaxRedirects());
            doFetch(responseCallback, requestStarted);
        } else {
            final String host = request.getURL().getHost();
            int port = request.getURL().getPort();
            port = port == -1 ? 80 : port;

            AsynchronousSocket returnSocket = socket;
            socket = null;

            // Return the socket to the socketpool for re-use
            pool.returnSocket(host, port, returnSocket);

            invokeResponseCallback();
        }
    }

    private void onChunk(final byte[] rawChunk) {
        final String chunk = new String(rawChunk, Charsets.ISO_8859_1);
        logger.debug("chunk size: {}", chunk.length());
        cancelTimeout();
        response.addChunk(chunk.substring(0, chunk.length() - NEWLINE.length()));
        startTimeout();
        socket.readUntil(NEWLINE.getBytes(), /* chunk delimiter */
        new NaiveAsyncResult() {
            public void onSuccess(final byte[] octet) {
                onChunkOctet(octet);
            }
        });
    }

    private void onChunkOctet(final byte[] rawOctet) {
        final String octet = new String(rawOctet, Charsets.ISO_8859_1);
        final int readBytes = Integer.parseInt(octet, 16);
        logger.debug("chunk octet: {} (decimal: {})", octet, readBytes);
        cancelTimeout();
        startTimeout();
        if (readBytes != 0) {
            socket.readBytes(readBytes + NEWLINE.length(), // chunk delimiter is
                    // \r\n
                    new NaiveAsyncResult() {
                        public void onSuccess(final byte[] chunk) {
                            onChunk(chunk);
                        }
                    });
        } else {
            onBody(response.getBody().getBytes(Charsets.ISO_8859_1));
        }
    }

    private void invokeResponseCallback() {
        final AsyncResult<Response> cb = responseCallback;
        responseCallback = nopAsyncResult;
        cb.onSuccess(response);
    }

    /**
     * Naive because all it does when an exception is thrown is log the
     * exception.
     */
    private abstract class NaiveAsyncResult implements AsyncResult<byte[]> {

        @Override
        public void onFailure(final Throwable caught) {
            logger.debug("onFailure: {}", caught);
        }

    }
}

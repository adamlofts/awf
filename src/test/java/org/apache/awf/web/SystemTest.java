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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.awf.configuration.Configuration;
import org.apache.awf.io.IOLoop;
import org.apache.awf.io.timeout.Timeout;
import org.apache.awf.web.SystemTestHandlers.*;
import org.apache.awf.web.http.client.AsynchronousHttpClient;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.apache.awf.web.SystemTestHandlers.*;

/**
 * General tests of server operation.
 */
public class SystemTest {

    private static final int PORT = 8081;

    @BeforeClass
    public static void setup() {

        final Configuration configuration = new Configuration();
        configuration.setCreateETags(true).setStaticDirectory("src/test/resources");
        configuration.addHandler("/", new ExampleRequestHandler());
        configuration.addHandler("/w", new WRequestHandler());
        configuration.addHandler("/ww", new WWRequestHandler());
        configuration.addHandler("/wwfw", new WWFWRequestHandler());
        configuration.addHandler("/wfwf", new WFWFRequestHandler());
        configuration.addHandler("/wfffwfff", new WFFFWFFFRequestHandler());
        configuration.addHandler("/delete", new DeleteRequestHandler());
        configuration.addHandler("/post", new PostRequestHandler());
        configuration.addHandler("/put", new PutRequestHandler());
        configuration.addHandler("/capturing/([0-9]+)", new CapturingRequestRequestHandler());
        configuration.addHandler("/throw", new ThrowingHttpExceptionRequestHandler());
        configuration.addHandler("/async_throw", new AsyncThrowingHttpExceptionRequestHandler());
        configuration.addHandler("/no_body", new NoBodyRequestHandler());
        configuration.addHandler("/moved_perm", new MovedPermanentlyRequestHandler());
        configuration.addHandler("/static_file_handler", new UserDefinedStaticContentHandler());
        configuration.addHandler("/450kb_body", new _450KBResponseEntityRequestHandler());
        configuration.addHandler("/echo", new EchoingPostBodyRequestHandler());
        configuration.addHandler("/authenticated", new AuthenticatedRequestHandler());
        configuration.addHandler("/query_params", new QueryParamsRequestHandler());
        configuration.addHandler("/chunked", new ChunkedRequestHandler());

        /*
         * Start server instance from a new thread because the start invocation
         * is blocking (invoking thread will be I/O loop thread).
         */
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                HttpServer server = new HttpServer(configuration);
                server.listen(PORT);
                IOLoop.INSTANCE.start();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {

        IOLoop.INSTANCE.addCallback(new AsyncCallback() {
            @Override
            public void onCallback() {
                IOLoop.INSTANCE.stop();
            }
        });
        Thread.sleep(300);
    }

    @Test
    public void simpleGetRequestTest() throws ClientProtocolException, IOException {
        doSimpleGetRequest();
    }

    /**
     * Test a RH that does a single write
     * 
     * @throws ClientProtocolException
     * @throws IOException
     */
    @Test
    public void wTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/w");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("1", payLoad);
        assertEquals(5, response.getAllHeaders().length);
        assertEquals("1", response.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void wwTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/ww");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("12", payLoad);
        assertEquals(5, response.getAllHeaders().length);
        assertEquals("2", response.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void wwfwTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wwfw");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("123", payLoad);
        assertEquals(3, response.getAllHeaders().length);
    }

    @Test
    public void wfwfTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wfwf");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("12", payLoad);
        assertEquals(3, response.getAllHeaders().length);
    }

    @Test
    public void wfffwfffTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wfffwfff");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("12", payLoad);
        assertEquals(3, response.getAllHeaders().length);
    }

    @Test
    public void deleteTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpDelete httpdelete = new HttpDelete("http://localhost:" + PORT + "/delete");
        HttpResponse response = httpclient.execute(httpdelete);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("delete", payLoad);
    }

    @Test
    public void PostTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpPost httppost = new HttpPost("http://localhost:" + PORT + "/post");
        HttpResponse response = httpclient.execute(httppost);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("post", payLoad);
    }

    @Test
    public void postWithContinueTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);
        params.setParameter("http.protocol.expect-continue", Boolean.TRUE);
        HttpClient httpclient = new DefaultHttpClient(params);
        HttpPost httppost = new HttpPost("http://localhost:" + PORT + "/post");
        HttpResponse response = httpclient.execute(httppost);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("post", payLoad);
    }

    @Test
    public void putTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpPut httpput = new HttpPut("http://localhost:" + PORT + "/put");
        HttpResponse response = httpclient.execute(httpput);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("put", payLoad);
    }

    @Test
    public void capturingTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/capturing/1911");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("/capturing/1911", payLoad);
    }

    @Test
    public void erroneousCapturingTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/capturing/r1911");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.CLIENT_ERROR_NOT_FOUND.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("Not Found", response.getStatusLine().getReasonPhrase());

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("Requested URL: /capturing/r1911 was not found", payLoad);
    }

    @Test
    public void simpleConcurrentGetRequestTest() {

        int nThreads = 8;
        int nRequests = 2048;
        final CountDownLatch latch = new CountDownLatch(nRequests);
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        for (int i = 1; i <= nRequests; i++) {
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        doSimpleGetRequest();
                        latch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
        }
        try {
            latch.await(15 * 1000, TimeUnit.MILLISECONDS); // max wait time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (latch.getCount() != 0) {
            assertTrue("Did not finish " + nRequests + " # of requests", false);
        }
    }

    @Test
    public void keepAliveRequestTest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Keep-Alive"));
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        DefaultHttpClient httpclient = new DefaultHttpClient(params);

        for (int i = 1; i <= 25; i++) {
            doKeepAliveRequestTest(httpclient);
        }
    }

    private void doKeepAliveRequestTest(DefaultHttpClient httpclient) throws IOException, ClientProtocolException {

        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals(expectedPayload, payLoad);
    }

    @Test
    public void HTTP_1_0_noConnectionHeaderTest() throws ClientProtocolException, IOException {

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, new ProtocolVersion("HTTP", 1, 0));
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals(expectedPayload, payLoad);
    }

    @Test
    public void httpExceptionTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/throw");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("Internal Server Error", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("exception message", payLoad);
    }

    @Test
    public void asyncHttpExceptionTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/async_throw");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("Internal Server Error", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("exception message", payLoad);
    }

    @Test
    public void staticFileRequestTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/src/test/resources/test.txt");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(7, response.getAllHeaders().length);

        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("test.txt", payLoad);
    }

    @Test
    public void pictureStaticFileRequestTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/src/test/resources/apache_feather.png");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(7, response.getAllHeaders().length);
        assertEquals("90048", response.getFirstHeader("Content-Length").getValue());
        
        // TODO: Correct this type!
        assertEquals("application/octet-stream", response.getFirstHeader("Content-Type").getValue());
        assertNotNull(response.getFirstHeader("Last-Modified"));
    }

    @Test
    public void pictureStaticLargeFileRequestTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/src/test/resources/f4_impact.jpg");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(7, response.getAllHeaders().length);
        assertEquals("image/jpeg", response.getFirstHeader("Content-Type").getValue());
        assertNotNull(response.getFirstHeader("Last-Modified"));
        // TODO RS 101026 Verify that the actual body/entity is 2145094 bytes
        // big (when we have support for "large" file)
    }

    @Test
    public void noBodyRequest() throws ClientProtocolException, IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/no_body");
        HttpResponse response = httpclient.execute(httpget);
        List<String> expectedHeaders = Arrays.asList(new String[] { "Server", "Date", "Content-Length", "Connection" });

        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

        for (String header : expectedHeaders) {
            assertTrue(response.getFirstHeader(header) != null);
        }

        assertEquals("", convertStreamToString(response.getEntity().getContent()).trim());
        assertEquals("0", response.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void movedPermanentlyRequest() throws ClientProtocolException, IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/moved_perm");
        HttpResponse response = httpclient.execute(httpget);
        List<String> expectedHeaders = Arrays.asList(new String[] { "Server", "Date", "Content-Length", "Connection",
                "Etag" });

        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

        for (String header : expectedHeaders) {
            assertTrue(response.getFirstHeader(header) != null);
        }

        assertEquals(expectedPayload, convertStreamToString(response.getEntity().getContent()).trim());
        assertEquals(expectedPayload.length() + "", response.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void sendGarbageTest() throws IOException {

        InetSocketAddress socketAddress = new InetSocketAddress(PORT);
        SocketChannel channel = SocketChannel.open(socketAddress);
        channel.write(ByteBuffer.wrap(new byte[] { 1, 1, 1, 1 } // garbage
                ));
        channel.close();
    }

    @Test
    public void userDefinedStaticContentHandlerTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/static_file_handler");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(4, response.getAllHeaders().length);
        assertEquals("8", response.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void timeoutTest() throws InterruptedException {

        long now = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(5);
        final AsyncCallback cb = new AsyncCallback() {

            @Override
            public void onCallback() {
                latch.countDown();
            }

        };

        Timeout t1 = new Timeout(now + 1000, cb);
        Timeout t2 = new Timeout(now + 1200, cb);
        Timeout t3 = new Timeout(now + 1400, cb);
        Timeout t4 = new Timeout(now + 1600, cb);
        Timeout t5 = new Timeout(now + 1800, cb);
        IOLoop.INSTANCE.addTimeout(t1);
        IOLoop.INSTANCE.addTimeout(t2);
        IOLoop.INSTANCE.addTimeout(t3);
        IOLoop.INSTANCE.addTimeout(t4);
        IOLoop.INSTANCE.addTimeout(t5);

        latch.await(5 * 1000, TimeUnit.MILLISECONDS);
        assertTrue(latch.getCount() == 0);
    }

    @Test
    public void callbackTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);
        final AsyncCallback cb = new AsyncCallback() {

            @Override
            public void onCallback() {
                latch.countDown();
            }

        };
        IOLoop.INSTANCE.addCallback(cb);
        IOLoop.INSTANCE.addCallback(cb);
        IOLoop.INSTANCE.addCallback(cb);
        IOLoop.INSTANCE.addCallback(cb);
        IOLoop.INSTANCE.addCallback(cb);

        latch.await(5 * 1000, TimeUnit.MILLISECONDS);
        assertTrue(latch.getCount() == 0);
    }

    // ning === http://github.com/ning/async-http-client
    @Test
    public void doSimpleAsyncRequestTestWithNing() throws IOException, InterruptedException {

        int iterations = 100;
        final CountDownLatch latch = new CountDownLatch(iterations);
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        for (int i = 1; i <= iterations; i++) {

            asyncHttpClient.prepareGet("http://localhost:" + PORT + "/").execute(
                    new AsyncCompletionHandler<com.ning.http.client.Response>() {

                        @Override
                        public com.ning.http.client.Response onCompleted(com.ning.http.client.Response response)
                                throws Exception {
                            String body = response.getResponseBody();
                            assertEquals(expectedPayload, body);
                            {
                                List<String> expectedHeaders = Arrays.asList(new String[] { "Server", "Date",
                                        "Content-Length", "Etag", "Connection" });
                                assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusCode());
                                assertEquals(expectedHeaders.size(), response.getHeaders().size());
                                for (String header : expectedHeaders) {
                                    assertTrue(response.getHeader(header) != null);
                                }
                                assertEquals(expectedPayload.length() + "", response.getHeader("Content-Length"));
                            }
                            latch.countDown();
                            return response;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            assertTrue(false);
                        }

                    });
        }
        latch.await(15 * 1000, TimeUnit.MILLISECONDS);
        assertEquals(0, latch.getCount());
    }

    // TODO 101108 RS enable when /mySql (AsyncDbHandler is properly
    // implemented)
    // ning === http://github.com/ning/async-http-client
    // @Test
    // public void doAsynchronousRequestTestWithNing() throws IOException,
    // InterruptedException {
    // int iterations = 200;
    // final CountDownLatch latch = new CountDownLatch(iterations);
    // AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    // for (int i = 1; i <= iterations; i++) {
    //
    // asyncHttpClient.prepareGet("http://localhost:" + PORT + "/mySql").
    // execute(new AsyncCompletionHandler<com.ning.http.client.Response>(){
    //
    // @Override
    // public com.ning.http.client.Response
    // onCompleted(com.ning.http.client.Response response) throws Exception{
    // String body = response.getResponseBody();
    // assertEquals("Name: Jim123", body);
    // List<String> expectedHeaders = Arrays.asList(new String[] {"Server",
    // "Date", "Content-Length", "Etag", "Connection"});
    // assertEquals(200, response.getStatusCode());
    // assertEquals(expectedHeaders.size(),
    // response.getHeaders().getHeaderNames().size());
    // for (String header : expectedHeaders) {
    // assertTrue(response.getHeader(header) != null);
    // }
    // assertEquals(""+ "Name: Jim123".length(),
    // response.getHeader("Content-Length"));
    // latch.countDown();
    // return response;
    // }
    //
    // @Override
    // public void onThrowable(Throwable t){
    // assertTrue(false);
    // }
    //
    // });
    // }
    // latch.await(15 * 1000, TimeUnit.MILLISECONDS);
    // assertEquals(0, latch.getCount());
    // }

    @Test
    public void _450KBEntityTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/450kb_body");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);
        // assertEquals(450*1024,
        // Integer.parseInt(response.getFirstHeader("Content-Length").getValue())/8);
        // assertEquals(450*1024,
        // _450KBResponseEntityRequestHandlr.entity.getBytes(Charsets.UTF_8).length);
        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals(_450KBResponseEntityRequestHandler.entity, payLoad);
    }

    @Test
    public void smallHttpPostBodyWithUnusualCharactersTest() throws ClientProtocolException, IOException {

        final String body = "Räger Schildmäijår";

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://localhost:" + PORT + "/echo");
        httppost.setEntity(new StringEntity(body)); // HTTP 1.1 says that the
        // default charset is
        // ISO-8859-1
        HttpResponse response = httpclient.execute(httppost);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);
        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals(body, payLoad);
    }

    @Test
    public void smallHttpPostBodyTest() throws ClientProtocolException, IOException, InterruptedException {

        final String body = "Roger Schildmeijer";
        final CountDownLatch latch = new CountDownLatch(1);
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.preparePost("http://localhost:" + PORT + "/echo").setBody(body).execute(
                new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        assertNotNull(response);
                        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusCode());
                        assertEquals("OK", response.getStatusText());
                        assertEquals(5, response.getHeaders().size());
                        String payLoad = response.getResponseBody();
                        assertEquals(body, payLoad);
                        latch.countDown();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                    }
                });

        latch.await();
        assertTrue(latch.getCount() == 0);
    }

    @Test
    public void largeHttpPostBodyTest() throws ClientProtocolException, IOException, InterruptedException {

        String body = "Roger Schildmeijer: 0\n";
        for (int i = 1; i <= 1000; i++) {
            body += "Roger Schildmeijer: " + i + "\n";
        }
        final String expectedBody = body;
        final CountDownLatch latch = new CountDownLatch(1);
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.preparePost("http://localhost:" + PORT + "/echo").setBody(body).execute(
                new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        assertNotNull(response);
                        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusCode());
                        assertEquals("OK", response.getStatusText());
                        assertEquals(5, response.getHeaders().size());
                        String payLoad = response.getResponseBody();
                        assertEquals(expectedBody, payLoad);
                        latch.countDown();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                    }
                });

        latch.await();
        assertTrue(latch.getCount() == 0);
    }

    @Test
    public void authenticatedRequestHandlerTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/authenticated");
        httpget.setHeader("user", "Roger Schildmeijer");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);
        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("Roger Schildmeijer", payLoad);
    }

    @Test
    public void notAuthenticatedRequestHandlerTest() throws ClientProtocolException, IOException {

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/authenticated");
        httpget.setHeader("wrong_header", "Roger Schildmeijer");
        HttpResponse response = httpclient.execute(httpget);

        assertNotNull(response);
        assertEquals(HttpStatus.CLIENT_ERROR_FORBIDDEN.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("Forbidden", response.getStatusLine().getReasonPhrase());
        assertEquals(5, response.getAllHeaders().length);
        String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
        assertEquals("Authentication failed", payLoad);
    }

    @Test
    public void queryParamsTest() throws ClientProtocolException, IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/query_params?key1=value1&key2=value2");
        HttpResponse response = httpclient.execute(httpget);
        List<String> expectedHeaders = Arrays.asList(new String[] { "Server", "Date", "Content-Length", "Etag",
                "Connection" });

        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

        for (String header : expectedHeaders) {
            assertTrue(response.getFirstHeader(header) != null);
        }

        final String expected = "value1 value2";
        assertEquals(expected, convertStreamToString(response.getEntity().getContent()).trim());
        assertEquals(expected.length() + "", response.getFirstHeader("Content-Length").getValue());
    }

    /**
     * TODO SLM This test does not make sense since stop use a callback and server store only one serverChannel
     * @Test
     */
    public void multipleStartStopCombinations() throws InterruptedException {

        Configuration configuration = new Configuration();
        configuration.setHandlerPackage(SystemTest.class.getPackage().getName());

        final HttpServer server = new HttpServer(configuration);

        final int n = 10;
        final CountDownLatch latch = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            IOLoop.INSTANCE.addCallback(new AsyncCallback() {
                public void onCallback() {
                    server.listen(PORT + 1);
                }
            });
            IOLoop.INSTANCE.addCallback(new AsyncCallback() {
                public void onCallback() {
                    server.stop();
                    latch.countDown();
                }
            });
        }
        latch.await(50, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void connectToUnresolvableAddressUsingAsynchronousHttpClient() throws InterruptedException {

        final String unresolvableAddress = "http://ttasfdqwertyuiop.se./start";
        final CountDownLatch latch = new CountDownLatch(1);
        final AsynchronousHttpClient client = new AsynchronousHttpClient();
        final AsyncCallback runByIOLoop = new AsyncCallback() {

            public void onCallback() {
                client.get(unresolvableAddress, new AsyncResult<org.apache.awf.web.http.client.Response>() {

                    public void onSuccess(org.apache.awf.web.http.client.Response result) {
                        client.close();
                    }

                    public void onFailure(Throwable caught) {
                        if (caught instanceof UnresolvedAddressException) {
                            latch.countDown();
                        }
                        client.close();
                    }
                });
            }
        };
        IOLoop.INSTANCE.addCallback(runByIOLoop);

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void connectToUnconnectableAddressUsingAsynchronousHttpClient() throws InterruptedException {

        final String unconnectableAddress = "http://localhost:8039/start";
        final CountDownLatch latch = new CountDownLatch(1);
        final AsynchronousHttpClient client = new AsynchronousHttpClient();
        final AsyncCallback runByIOLoop = new AsyncCallback() {

            public void onCallback() {
                client.get(unconnectableAddress, new AsyncResult<org.apache.awf.web.http.client.Response>() {

                    public void onSuccess(org.apache.awf.web.http.client.Response result) {
                        client.close();
                    }

                    public void onFailure(Throwable caught) {
                        if (caught instanceof ConnectException) {
                            latch.countDown();
                        }
                        client.close();
                    }
                });
            }
        };
        IOLoop.INSTANCE.addCallback(runByIOLoop);

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void multipleAsynchronousHttpClientTest() throws InterruptedException {

        for (int i = 0; i < 100; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final String url = "http://localhost:" + PORT + "/";
            final AsynchronousHttpClient http = new AsynchronousHttpClient();
            final String[] result = { "BODY_PLACEHOLDER", "STATUSCODE_PLACEHOLDER" };
            final AsyncResult<org.apache.awf.web.http.client.Response> cb = new AsyncResult<org.apache.awf.web.http.client.Response>() {

                public void onSuccess(org.apache.awf.web.http.client.Response response) {
                    result[0] = response.getBody();
                    result[1] = response.getStatusLine();
                    latch.countDown();
                }

                public void onFailure(Throwable ignore) {
                }
            };
            // make sure that the http.fetch(..) is invoked from the ioloop
            // thread
            IOLoop.INSTANCE.addCallback(new AsyncCallback() {
                public void onCallback() {
                    http.get(url, cb);
                }
            });
            latch.await(30, TimeUnit.SECONDS);
            assertEquals(0, latch.getCount());
            assertEquals("hello test", result[0]);
            assertEquals("HTTP/1.1 200 OK", result[1]);
        }
    }

    @Test
    public void AsynchronousHttpClientConnectionFailedTest() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final String url = "http://localhost:" + (PORT + 1) + "/";
        final AsynchronousHttpClient http = new AsynchronousHttpClient();
        final AsyncResult<org.apache.awf.web.http.client.Response> cb = new AsyncResult<org.apache.awf.web.http.client.Response>() {

            public void onSuccess(org.apache.awf.web.http.client.Response response) {
            }

            public void onFailure(Throwable e) {
                if (e instanceof ConnectException) {
                    latch.countDown();
                }
            }
        };
        // make sure that the http.fetch(..) is invoked from the ioloop thread
        IOLoop.INSTANCE.addCallback(new AsyncCallback() {
            public void onCallback() {
                http.get(url, cb);
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void AsynchronousHttpClientRedirectTest() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        // final String url = "http://localhost:" + (PORT) + "/moved_perm";
        final String url = "http://localhost:" + PORT + "/moved_perm";
        final AsynchronousHttpClient http = new AsynchronousHttpClient();
        final AsyncResult<org.apache.awf.web.http.client.Response> cb = new AsyncResult<org.apache.awf.web.http.client.Response>() {

            public void onSuccess(org.apache.awf.web.http.client.Response response) {
                if (response.getBody().equals(expectedPayload)) {
                    latch.countDown();
                }
            }

            public void onFailure(Throwable e) {
            }

        };
        // make sure that the http.fetch(..) is invoked from the ioloop thread
        IOLoop.INSTANCE.addCallback(new AsyncCallback() {
            public void onCallback() {
                http.get(url, cb);
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void serverChunkedBodyRequest()throws Exception{
        HttpClient httpclient   = new DefaultHttpClient();
        HttpPost httpPost       = new HttpPost("http://localhost:" + PORT + "/echo");
        StringEntity se         = new StringEntity("azertyuiopqsdfghjklmwxc\nvbn1234567890");
        se.setChunked(true);
        httpPost.setEntity(se);

        HttpResponse httpResponse = httpclient.execute(httpPost);
        assertEquals("azertyuiopqsdfghjklmwxc\nvbn1234567890",new String(EntityUtils.toByteArray(httpResponse.getEntity())));
    }

    @Test
    public void asynchronousHttpClientTransferEncodingChunkedTest() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final String url = "http://localhost:" + PORT + "/chunked";
        final AsynchronousHttpClient http = new AsynchronousHttpClient();
        final AsyncResult<org.apache.awf.web.http.client.Response> cb = new AsyncResult<org.apache.awf.web.http.client.Response>() {

            public void onSuccess(org.apache.awf.web.http.client.Response response) {
                if (response.getBody().equals("arogerab") && response.getHeader("Transfer-Encoding").equals("chunked")) {
                    latch.countDown();
                }
            }

            public void onFailure(Throwable e) {
            }

        };
        // make sure that the http.fetch(..) is invoked from the ioloop thread
        IOLoop.INSTANCE.addCallback(new AsyncCallback() {
            public void onCallback() {
                http.get(url, cb);
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    private void doSimpleGetRequest() throws ClientProtocolException, IOException {

        List<Header> headers = new LinkedList<Header>();
        headers.add(new BasicHeader("Connection", "Close"));

        HttpParams params = new BasicHttpParams();
        params.setParameter("http.default-headers", headers);

        HttpClient httpclient = new DefaultHttpClient(params);
        HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = httpclient.execute(httpget);
        List<String> expectedHeaders = Arrays.asList(new String[] { "Server", "Date", "Content-Length", "Etag",
                "Connection" });

        assertEquals(HttpStatus.SUCCESS_OK.code(), response.getStatusLine().getStatusCode());
        assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
        assertEquals("OK", response.getStatusLine().getReasonPhrase());

        assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

        for (String header : expectedHeaders) {
            assertTrue(response.getFirstHeader(header) != null);
        }

        assertEquals(expectedPayload, convertStreamToString(response.getEntity().getContent()).trim());
        assertEquals(expectedPayload.length() + "", response.getFirstHeader("Content-Length").getValue());
    }

    private String convertStreamToString(InputStream is) throws IOException {

        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }

            return sb.toString();
        }

        return "";
    }
}

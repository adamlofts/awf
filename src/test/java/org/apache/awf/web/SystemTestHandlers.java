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

import java.io.File;

import org.apache.awf.annotation.Asynchronous;
import org.apache.awf.annotation.Authenticated;
import org.apache.awf.web.AsyncResult;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.HttpException;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.protocol.HttpStatus;

/**
 * A collection of <code>RequestHandler</code>s used by
 * <code>SystemTest</code>.
 */
public class SystemTestHandlers {

    public static final String expectedPayload = "hello test";

    public static class ExampleRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(expectedPayload);
        }
    }

    public static class WRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("1");
        }
    }

    public static class WWRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("1");
            response.write("2");
        }
    }

    public static class WWFWRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("1");
            response.write("2");
            response.flush();
            response.write("3");
        }
    }

    public static class WFWFRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("1");
            response.flush();
            response.write("2");
            response.flush();
        }
    }

    public static class WFFFWFFFRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("1");
            response.flush();
            response.flush();
            response.flush();
            response.write("2");
            response.flush();
            response.flush();
            response.flush();
        }
    }

    public static class DeleteRequestHandler extends RequestHandler {
        @Override
        public void delete(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("del");
            response.flush();
            response.write("ete");
            response.flush();
        }
    }

    public static class PostRequestHandler extends RequestHandler {
        @Override
        public void post(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("po");
            response.flush();
            response.write("st");
            response.flush();
        }
    }

    public static class PutRequestHandler extends RequestHandler {
        @Override
        public void put(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write("p");
            response.flush();
            response.write("ut");
            response.flush();
        }
    }

    public static class CapturingRequestRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(request.getRequestedPath());
        }
    }

    public static class ThrowingHttpExceptionRequestHandler extends RequestHandler {
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            throw new HttpException(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR, "exception message");
        }
    }

    public static class AsyncThrowingHttpExceptionRequestHandler extends RequestHandler {
        @Asynchronous
        @Override
        public void get(org.apache.awf.web.http.HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            throw new HttpException(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR, "exception message");
        }
    }

    public static class NoBodyRequestHandler extends RequestHandler {
        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.setStatus(HttpStatus.SUCCESS_OK);
        }
    }

    public static class MovedPermanentlyRequestHandler extends RequestHandler {
        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.setStatus(HttpStatus.REDIRECTION_MOVED_PERMANENTLY);
            response.setHeader("Location", "/");
        }
    }

    public static class UserDefinedStaticContentHandler extends RequestHandler {
        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(new File("src/test/resources/test.txt"));
        }
    }

    public static class _450KBResponseEntityRequestHandler extends RequestHandler {
        public static String entity;

        static {
            int iterations = 450 * 1024;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= iterations; i++) {
                sb.append("a");
            }
            entity = sb.toString();
        }

        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(entity);
        }
    }

    public static class EchoingPostBodyRequestHandler extends RequestHandler {
        @Override
        public void post(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(request.getBody());
        }
    }

    public static class AuthenticatedRequestHandler extends RequestHandler {
        @Override
        @Authenticated
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(request.getHeader("user"));
        }

        @Override
        public String getCurrentUser(HttpRequest request) {
            return request.getHeader("user");
        }
    }

    public static class QueryParamsRequestHandler extends RequestHandler {
        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.write(request.getParameter("key1") + " " + request.getParameter("key2"));
        }
    }

    public static class ChunkedRequestHandler extends RequestHandler {
        @Override
        public void get(HttpRequest request, org.apache.awf.web.http.HttpResponse response) {
            response.setHeader("Transfer-Encoding", "chunked");
            sleep(300);

            response.write("1\r\n");
            response.write("a\r\n").flush();
            sleep(300);

            response.write("5\r\n");
            response.write("roger\r\n").flush();
            sleep(300);

            response.write("2\r\n");
            response.write("ab\r\n").flush();
            sleep(300);

            response.write("0\r\n");
            response.write("\r\n");
        }

        public static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignore) { /* nop */
            }
        }
    }
}

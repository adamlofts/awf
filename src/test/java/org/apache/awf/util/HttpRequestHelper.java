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
package org.apache.awf.util;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This class can be used to create HttpRequests (and corresponding byte
 * representations)
 */
public class HttpRequestHelper {

    // Default request will look like this:
    /*
     * GET / HTTP/1.0 Host: localhost:8080 User-Agent: Mozilla/5.0 From:
     * abcde@qwert.com
     */

    enum ParameterDelimMode {
        AMPERSAND, SEMICOLON, MIXED
    }

    private ParameterDelimMode paramDelimMode = ParameterDelimMode.MIXED;

    private String protocol = "HTTP";
    private String method = "GET";
    private String version = "1.1";
    private String requestedPath = "/";
    private Map<String, String> headers = new HashMap<String, String>();
    private Multimap<String, String> getParameters = HashMultimap.create();

    public HttpRequestHelper() {
        headers.put("Host", "localhost:8080");
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("From", "abcde@qwert.com");
    }

    public String getRequestAsString() {
        String requestLine = createRequestLine();
        String headerString = createHeaders();
        // TODO Body
        String request = requestLine + headerString;
        return request;
    }

    public byte[] getRequestAsBytes() {
        String request = getRequestAsString();
        return request.getBytes();
    }

    public ByteBuffer getRequestAsByteBuffer() {
        return ByteBuffer.wrap(getRequestAsBytes());
    }

    public String addHeader(String name, String value) {
        return headers.put(name, value);
    }

    public String removeHeader(String name) {
        return headers.remove(name);
    }

    public boolean addGetParameter(String name, String value) {
        return getParameters.put(name, value);
    }

    public void setRequestedPath(String path) {
        requestedPath = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setParameterDelimMode(ParameterDelimMode mode) {
        paramDelimMode = mode;
    }

    private String getParameterDelimiter() {
        String delim;
        switch (paramDelimMode) {
        case AMPERSAND:
            delim = "&";
            break;
        case SEMICOLON:
            delim = ";";
            break;
        case MIXED:
            if (Math.random() > 0.5) {
                delim = "&";
            } else {
                delim = ";";
            }
            break;
        default:
            delim = ";";
        }
        return delim;
    }

    /**
     * Creates the initial request line, i.e: GET / HTTP/1.0
     * 
     * It also add \r\n to the end of the line
     */
    private String createRequestLine() {
        String requestedPathWithParams = requestedPath;

        if (!getParameters.isEmpty()) { // Add get parameters
            requestedPathWithParams += "?";
            for (String paramName : getParameters.keySet()) {
                String delimiter = getParameterDelimiter();
                Collection<String> values = getParameters.get(paramName);
                for (String value : values) { // A single param can have
                                              // multiple values
                    String val = value == null ? "" : value;
                    requestedPathWithParams += paramName + "=" + val + delimiter;
                }
            }
            // Remove last &
            requestedPathWithParams = requestedPathWithParams.substring(0, requestedPathWithParams.length() - 1);
        }
        String reqLine = method + " " + requestedPathWithParams + " " + protocol + "/" + version + "\r\n";
        return reqLine;
    }

    /**
     * Creates the header lines, i.e: Host: localhost:8080 User-Agent:
     * Mozilla/5.0 From: abcde@qwert.com
     * 
     * It also add \r\n to the end of the line
     */
    private String createHeaders() {
        String result = "";
        for (String headerKey : headers.keySet()) {
            String headerValue = headers.get(headerKey);
            result += headerKey + ": " + headerValue + "\r\n";
        }
        result += "\r\n";
        return result;
    }
}

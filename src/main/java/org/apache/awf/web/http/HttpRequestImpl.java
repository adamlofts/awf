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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.apache.awf.web.http.protocol.HttpVerb;

/**
 *
 */
public class HttpRequestImpl implements HttpRequest {

    private  String requestLine;
    private  HttpVerb method;
    private  String requestedPath; // correct name?
    private  String version;
    private Map<String, String> headers;
    private ImmutableMultimap<String, String> parameters;
    private String body;
    private boolean keepAlive;
    private InetAddress remoteHost;
    private InetAddress serverHost;
    private int remotePort;
    private int serverPort;
    private Map<String, String> cookies = null;
    private final HttpParsingContext context = new HttpParsingContext();
    private int contentLength = -1;
    private int chunkedSize = 0;
    private DynamicByteBuffer bodyBuffer;


    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");
    /** Regex to parse out QueryString from HttpRequest */
    public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");
    /** Regex to parse out parameters from query string */
    public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;");
    /** Regex to parse out key/value pairs */
    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");
    /** Regex to split cookie header following RFC6265 Section 5.4 */
    public static final Pattern COOKIE_SEPARATOR_PATTERN = Pattern.compile(";");


    public HttpRequestImpl(){
        headers = Maps.newHashMap();
    }

    /**
     * Creates a new HttpRequest
     * 
     * @param requestLine The Http request text line
     * @param headers The Http request headers
     */
    public HttpRequestImpl(String requestLine, Map<String, String> headers) {
        this.requestLine = requestLine;
        String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        method = HttpVerb.valueOf(elements[0]);
        String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        requestedPath = pathFrags[0];
        version = elements[2];
        this.headers = headers;
        body = null;
        initKeepAlive();
        parameters = parseParameters((pathFrags.length>1 ? pathFrags[1]: ""));
    }

    @Override
    public String getRequestLine() {
        return requestLine;
    }

    @Override
    public String getRequestedPath() {
        return requestedPath;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public HttpVerb getMethod() {
        return method;
    }

    /**
     * Returns the value of a request parameter as a String, or null if the
     * parameter does not exist.
     * 
     * You should only use this method when you are sure the parameter has only
     * one value. If the parameter might have more than one value, use
     * getParameterValues(java.lang.String). If you use this method with a
     * multi-valued parameter, the value returned is equal to the first value in
     * the array returned by getParameterValues.
     */
    @Override
    public String getParameter(String name) {
        Collection<String> values = parameters.get(name);
        return values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public Map<String, Collection<String>> getParameters() {
        return parameters.asMap();
    }

    @Override
    public String getBody() {

        if(bodyBuffer != null){
            return new String (bodyBuffer.array(), 0, bodyBuffer.position(), Charsets.ISO_8859_1);
        }else {
            return body;
        }
    }



    @Override
    public InetAddress getRemoteHost() {
        return remoteHost;
    }

    @Override
    public InetAddress getServerHost() {
        return serverHost;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    protected void setRemoteHost(InetAddress host) {
        remoteHost = host;
    }

    protected void setServerHost(InetAddress host) {
        serverHost = host;
    }

    protected void setRemotePort(int port) {
        remotePort = port;
    }

    protected void setServerPort(int port) {
        serverPort = port;
    }

    /**
     * Returns a map with all cookies contained in the request. Cookies are
     * represented as strings, and are parsed at the first invocation of this
     * method
     * 
     * @return a map containing all cookies of request
     */
    @Override
    public Map<String, String> getCookies() {
        if (cookies == null) {
            parseCookies();
        }
        return Collections.unmodifiableMap(cookies);
    }

    /**
     * Returns a given cookie. Cookies are represented as strings, and are
     * parsed at the first invocation of this method
     * 
     * @param name the name of cookie
     * @return the corresponding cookie, or null if the cookie does not exist
     */
    @Override
    public String getCookie(String name) {
        if (cookies == null) {
            parseCookies();
        }
        return cookies.get(name);
    }

    /**
     * Returns a collection of all values associated with the provided
     * parameter. If no values are found an empty collection is returned.
     */
    @Override
    public Collection<String> getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * TODO SLM This should output the real request and use a StringBuilder
     * @return
     */
    @Override
    public String toString() {
        String result = "METHOD: " + method + "\n";
        result += "VERSION: " + version + "\n";
        result += "PATH: " + requestedPath + "\n";

        result += "--- HEADER --- \n";
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            result += key + ":" + value + "\n";
        }

        result += "--- PARAMETERS --- \n";
        for (String key : parameters.keySet()) {
            Collection<String> values = parameters.get(key);
            for (String value : values) {
                result += key + ":" + value + "\n";
            }
        }
        if (getBody() != null) {
        	result += "--- BODY --- \n";
            result += getBody();
        }
        
        return result;
    }

    private ImmutableMultimap<String, String> parseParameters(String params) {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();

            String[] paramArray = PARAM_STRING_PATTERN.split(params);
            for (String keyValue : paramArray) {
                String[] keyValueArray = KEY_VALUE_PATTERN.split(keyValue);
                // We need to check if the parameter has a value associated with
                // it.
                if (keyValueArray.length > 1) {
                	String value = keyValueArray[1];
                	try {
                		value = URLDecoder.decode(value, "UTF-8");
                	} catch (UnsupportedEncodingException e) {
                		// Should not happen
                	}
                	builder.put(keyValueArray[0], value);
                }
            }

        return builder.build();
    }

    /**
     * Parse the cookie's http header (RFC6265 Section 5.4)
     */
    private void parseCookies() {
        String cookiesHeader = Strings.nullToEmpty(getHeader("Cookie")).trim();
        cookies = Maps.newHashMap();
        if (!cookiesHeader.equals("")) {
            String[] cookiesStrings = COOKIE_SEPARATOR_PATTERN.split(cookiesHeader);
            for (String cookieString : cookiesStrings) {
                String[] cookie = KEY_VALUE_PATTERN.split(cookieString, 2);
                cookies.put(cookie[0].trim(), cookie[1].trim());
            }
        }
    }

    protected void initKeepAlive() {
        keepAlive = true;
        String connection = getHeader("Connection");
        if ("close".equalsIgnoreCase(connection) || requestLine.contains("1.0")) {
            keepAlive = false;
        }
    }


    protected HttpParsingContext getContext(){
        return this.context;
    }

    protected void setMethod(HttpVerb method) {
        this.method = method;
    }

    /**
     * Sets the requestedPath and parse parameters using the received complete URI
     * @param uri
     */
    protected void setURI(String uri) {
        String[] pathFrags = QUERY_STRING_PATTERN.split(uri);
        requestedPath = pathFrags[0];
        parameters = parseParameters((pathFrags.length > 1 ? pathFrags[1] : ""));

        requestLine = method.toString() + " "+ uri;

    }

    protected void setVersion(String version) {
        this.version = version;
        requestLine += " " + version;
    }

    /**
     * Append the given value to the specified header.
     * If the header does not exist it will be added to the header map.
     */
    protected void pushToHeaders(String name, String value) {
        if (name != null){
			name = name.toLowerCase();
			// Handle repeated header-name like Cookies
			if (headers.containsKey(name)){
				value = new StringBuilder(headers.get(name)).append(';').append(value.trim()).toString();
			}
			headers.put(name, value.trim());
		}
    }

    /**
     * compute contentLength with header content-length when needed.
     * Please notice that it will also allocate the body buffer to the appropriate size.
     * @return actual content length or 0 if not specified
     */
    public int getContentLength(){
        if (contentLength < 0 ){
            if (headers.containsKey("content-length")){
                contentLength = Integer.parseInt(headers.get("content-length"));
                bodyBuffer = DynamicByteBuffer.allocate(contentLength);
            }else {
                contentLength = 0;
            }
        }
        return contentLength;
    }

    /**
     * Check wether this request body uses chunked encoding
     * @return
     */
    public boolean isChunked(){
        String te =headers.get("transfer-encoding");
        if (te != null){
           return te.indexOf("chunked") > -1;
        }
        return false;
    }


    protected void incrementChunkSize(int size){
        chunkedSize += size;
    }

    protected void buildChunkedBody(){
        bodyBuffer = DynamicByteBuffer.allocate(HttpServerDescriptor.READ_BUFFER_SIZE);
    }

    protected DynamicByteBuffer getBodyBuffer(){
        return bodyBuffer;
    }

    protected boolean isFinished(){
        boolean res = context.isbodyFound();
        if (res ){
            if (contentLength > 0){
                res = contentLength <= bodyBuffer.position();
            }else if (isChunked()){
                res = context.chunked;
            }
        }

        return res;
    }
    
    public boolean expectContinue() {    	
    	return (bodyBuffer == null || bodyBuffer.position() == 0) && headers.containsKey("expect");
    }

}

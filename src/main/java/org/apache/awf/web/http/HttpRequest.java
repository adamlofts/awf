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

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;

import org.apache.awf.web.http.protocol.HttpVerb;

/**
 * An HTTP request received from a client
 */
public interface HttpRequest {

    /**
     * Get the HTTP request line for the request.
     * <p>
     * Ex :
     * 
     * <pre>
     * GET http://www.w3.org/pub/WWW/TheProject.html HTTP/1.1
     * </pre>
     * 
     * @return the current request line.
     */
    public String getRequestLine();

    /**
     * The path of this request
     * <p>
     * Ex :
     * 
     * <pre>
     * http://www.w3.org/pub/WWW/TheProject.html
     * </pre>
     * 
     * @return the path requested
     */
    public String getRequestedPath();

    /**
     * The version of the HTTP protocol.
     * <p>
     * Can be <tt>HTTP/1.0</tt> or <tt>HTTP/1.1</tt>.
     * 
     * @return the HTTP version
     */
    public String getVersion();

    /**
     * Get the read-only header of this request.
     * 
     * @see HttpRequest#getHeader(String)
     * 
     * @return the header.
     */
    public Map<String, String> getHeaders();

    /**
     * Get the value of a given HTTP header.
     * 
     * @see HttpRequest#getHeaders()
     * @param name the name of the requested header
     * @return the value or <code>null</code> if the header is not found.
     */
    public String getHeader(String name);

    /**
     * The method (POST,GET ..) used for this request.
     * 
     * @see HttpVerb
     * 
     * @return the method
     */
    public HttpVerb getMethod();

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
    public String getParameter(String name);

    /**
     * Returns a map of all parameters where each key is a parameter name,
     * linked value is a Collection of String containing all known values for the parameter.
     * When the parameter has no value, the collection will be empty.
     * 
     * @return all the request parameters
     */
    public Map<String, Collection<String>> getParameters();

    /**
     * Returns a collection of all values associated with the provided
     * parameter. If no values are found and empty collection is returned.
     */
    public Collection<String> getParameterValues(String name);

    /**
     * The body of this request
     * 
     * @return the body as a {@link String}
     */
    public String getBody();

    /**
     * The address of the client which issued this request.
     * 
     * @return the address
     */
    public InetAddress getRemoteHost();

    /**
     * The TCP port of the client which issued this request.
     * 
     * @return the remote port number.
     */
    public int getRemotePort();

    /**
     * The address of the server which received this request.
     * 
     * @return an <code>InetAddress</code> representing the server address.
     */
    public InetAddress getServerHost();

    /**
     * The TCP port of the server which received this request.
     * 
     * @return the server port number.
     */
    public int getServerPort();

    /**
     * Returns a map with all cookies contained in the request. Cookies are
     * represented as strings, and are parsed at the first invocation of this
     * method
     * 
     * @return a map containing all cookies of request
     */
    public Map<String, String> getCookies();

    /**
     * Returns a given cookie. Cookies are represented as strings, and are
     * parsed at the first invocation of this method
     * 
     * @param name the name of cookie
     * @return the corresponding cookie, or null if the cookie does not exist
     */
    public String getCookie(String name);

    /**
     * Does keep-alive was requested.
     * 
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html">HTTP/1.1
     *      persistent connections</a>
     * @return <code>true</code> if keep-alive requested
     */
    public boolean isKeepAlive();
    
    /**
     * Check if the request expect a response with 100 Continue header
     * 
     * @return
     */
    public boolean expectContinue();

}

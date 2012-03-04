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

import java.io.File;

import org.apache.awf.web.http.protocol.HttpStatus;

/**
 * An HTTP response build and sent to a client in response to a
 * {@link HttpRequest}
 */
public interface HttpResponse {

    /**
     * Set the HTTP Response status (return) code, such as 200 for success.
     * 
     * @param status the <code>HttpStatus</code> to apply.
     */
    void setStatus(HttpStatus status);

    /**
     * Set an HTTP header value. If the header doesn't exist, it will be added.
     * 
     * @param header the unique header key
     * @param value the string value
     */
    void setHeader(String header, String value);

    /**
     * Set whether ETags should be generated and applied. By default, they are
     * not.
     * 
     * @param create <code>true</code> to generate and apply; <code>false</code>
     *            otherwise.
     */
    void setCreateETag(boolean create);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @param expiration expiration of cookie in seconds
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, long expiration);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @param domain cookie domain
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, String domain);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @param domain cookie domain
     * @param path cookie path
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, String domain, String path);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @param expiration expiration of cookie in seconds
     * @param domain cookie domain
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, long expiration, String domain);

    /**
     * Add a cookie to response.
     * 
     * @see #setCookie(String, String, long, String, String, boolean, boolean)
     * @param name name of cookie
     * @param value value of cookie
     * @param expiration expiration of cookie in seconds
     * @param domain cookie domain
     * @param path cookie path
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, long expiration, String domain, String path);

    /**
     * Add a cookie to response. The addition of a cookie with a name that was
     * already inserted, causes the substitution of the previous cookie
     * 
     * @param name name of cookie
     * @param value value of cookie
     * @param expiration expiration of cookie in seconds
     * @param domain cookie domain
     * @param path cookie path
     * @param secure set <code>Secure</code> property of cookie
     * @param httpOnly set <code>HttpOnly</code> property of cookie
     * @throws IllegalArgumentException if cookie name, or value, is not valid
     */
    void setCookie(String name, String value, long expiration, String domain, String path, boolean secure,
            boolean httpOnly);

    /**
     * Removes a cookie. This method forces the removal of a cookie, by setting
     * its expiration in the past.
     * 
     * @param name name of cookie to delete
     */
    void clearCookie(String name);

    /**
     * The given data data will be sent as the HTTP response upon next flush or
     * when the response is finished.
     * 
     * @return this for chaining purposes.
     */
    HttpResponse write(String data);

    /**
     * The given data data will be sent as the HTTP response upon next flush or
     * when the response is finished.
     * 
     * @param data the data to write.
     * @return <code>this</code>, for chaining.
     */
    HttpResponse write(byte[] data);

    /**
     * Experimental support.
     */
    long write(File file);

    /**
     * Explicit flush.
     * 
     * @return the number of bytes that were actually written as the result of
     *         this flush.
     */
    long flush();

    /**
     * Should only be invoked by third party asynchronous request handlers (or
     * by the AWF framework for synchronous request handlers). If no previous
     * (explicit) flush is invoked, the "Content-Length" and (where configured)
     * "ETag" header will be calculated and inserted to the HTTP response.
     * 
     * @see #setCreateETag(boolean)
     */
    long finish();
}

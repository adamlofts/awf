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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.awf.web.http.protocol.ContentType;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Request {

    /** The <code>Logger</code>. */
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    /** The <code>URL</code> associated with the current <code>Request</code>. */
    private final URL url;

    /** The request type. */
    private final HttpVerb verb;

    /**
     * Indicates whether 3xx Redirection codes should be followed; defaults to
     * <code>true</code>.
     * 
     * @see AsynchronousHttpClient#onBody
     */
    private boolean followRedirects = true;

    /**
     * The maximum number of redirects to follow, where enabled; defaults to 7.
     * 
     * @see #followRedirects
     */
    private int maxRedirects = 7;

    /** The body associated with the request. */
    private byte[] body;

    /** The type of content represented by this request. */
    private String contentType = ContentType.APPLICATION_FORM_URLENCODED;

    /**
     * Create an instance of this type with the given <code>URL</code> and
     * <code>HttpVerb</code>. Follows redirects and to a count as specified by
     * default.
     * 
     * @throws RuntimeException
     *             where a {@link MalformedURLException} is caught.
     * @see #Request(String, HttpVerb, boolean, int)
     */
    public Request(final String url, final HttpVerb verb) {
        try {
            this.url = new URL(url);
            this.verb = verb;
        } catch (final MalformedURLException e) {
            logger.error("Malformed URL: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an instance of this type with the given <code>URL</code> and
     * <code>HttpVerb</code>. Redirection behaviour and count are as specified.
     * 
     * @param url
     *            the <code>URL</code> for the request.
     * @param verb
     *            the type of request to be made.
     * @param followRedirects
     *            <code>true</code> if redirects are to be followed;
     *            <code>false</code> otherwise.
     * @param maxRedirects
     *            where redirects should be followed, the maximum number to
     *            follow.
     * @see #Request(String, HttpVerb)
     * @see AsynchronousHttpClient#onBody
     */
    public Request(final String url, final HttpVerb verb, final boolean followRedirects, final int maxRedirects) {
        this(url, verb);
        this.followRedirects = followRedirects;
        this.maxRedirects = maxRedirects;
    }

    /**
     * Retrieve the <code>URL</code> associated with this <code>Request</code>.
     * 
     * @return the associated <code>URL</code>.
     */
    public URL getURL() {
        return this.url;
    }

    /**
     * Retrieve the current type of request.
     * 
     * @return the associated <code>HttpVerb</code>.
     */
    public HttpVerb getVerb() {
        return this.verb;
    }

    /**
     * Indicates whether 3xx Redirection codes should be followed.
     * 
     * @return <code>true</code> if redirects should be followed;
     *         <code>false</code> otherwise.
     * @see #followRedirects
     */
    public boolean isFollowingRedirects() {
        return this.followRedirects;
    }

    /**
     * Retrieve the maximum number of redirects this <code>Request</code> will
     * follow.
     * 
     * @return an <code>int</code> representing the number of redirects.
     */
    public int getMaxRedirects() {
        return this.maxRedirects;
    }

    /**
     * Retrieve the body associated with this <code>Request</code>.
     * 
     * @return the associated body; may be <code>null</code>.
     */
    public byte[] getBody() {
        return this.body;
    }

    /**
     * Set the body applicable to this <code>Request</code>.
     * 
     * @param body
     *            the body to apply.
     */
    public void setBody(final byte[] body) {
        this.body = body;
    }

    /**
     * Set the body applicable to this <code>Request</code>.
     * 
     * @param body
     *            the body to apply.
     */
    public void setBody(final String body) {
        this.body = body.getBytes();
    }

    /**
     * Retrieve the content type associated with this <code>Request</code>.
     * 
     * @return the associated content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set the content type associated with this <code>Request</code>.
     * 
     * @param contentType
     *            the content type to apply.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

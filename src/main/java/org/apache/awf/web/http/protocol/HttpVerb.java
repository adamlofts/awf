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
package org.apache.awf.web.http.protocol;

/**
 * An <code>Enumeration</code> of all available HTTP verbs, as defined by <a
 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">Hypertext
 * Transfer Protocol -- HTTP/1.1 (RFC 2616)</a>.
 */
public enum HttpVerb {

    /**
     * &quot;The OPTIONS method represents a request for information about the
     * communication options available on the request/response chain identified
     * by the Request-URI.&quot; (RFC 2616, 9.2)
     */
    OPTIONS,
    /**
     * &quotThe GET method means retrieve whatever information (in the form of
     * an entity) is identified by the Request-URI.&quot; (RFC 2616, 9.3)
     */
    GET,
    /**
     * &quot;The HEAD method is identical to GET except that the server MUST NOT
     * return a message-body in the response.&quot; (RFC 2616, 9.4)
     */
    HEAD,
    /**
     * &quot;The POST method is used to request that the origin server accept
     * the entity enclosed in the request as a new subordinate of the resource
     * identified by the Request-URI in the Request-Line.&quot; (RFC 2616, 9.5)
     */
    POST,
    /**
     * &quot;The PUT method requests that the enclosed entity be stored under
     * the supplied Request-URI.&quot; (RFC 2616, 9.6)
     */
    PUT,
    /**
     * &quot;The DELETE method requests that the origin server delete the
     * resource identified by the Request-URI.&quot; (RFC 2616, 9.7)
     */
    DELETE,
    /**
     * &quot;The TRACE method is used to invoke a remote, application-layer
     * loop- back of the request message.&quot; (RFC 2616, 9.8)
     */
    TRACE,
    /**
     * &quot;This specification reserves the method name CONNECT for use with a
     * proxy that can dynamically switch to being a tunnel &quot; (RFC 2616,
     * 9.9)
     */
    CONNECT;
}

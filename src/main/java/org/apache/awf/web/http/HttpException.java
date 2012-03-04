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

import org.apache.awf.web.http.protocol.HttpStatus;

/**
 * Representation of an exception thrown by the server through the HTTP
 * protocol.
 */
public class HttpException extends RuntimeException {

    /** Serial Version UID */
    private static final long serialVersionUID = 8066634515515557043L;

    /** The HTTP status for this exception. */
    private final HttpStatus status;

    /**
     * Create an instance of this type, with the given <code>HttpStatus</code>
     * and an empty message.
     * 
     * @param status the <code>HttpStatus</code> to apply.
     */
    public HttpException(HttpStatus status) {
        this(status, "");
    }

    /**
     * Create an instance of this type, with the given <code>HttpStatus</code>
     * and message.
     * 
     * @param status the <code>HttpStatus</code> to apply.
     */
    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Retrieve the <code>HttpStatus</code> represented by this exception.
     * 
     * @return the represented <code>HttpStatus</code>.
     */
    public HttpStatus getStatus() {
        return status;
    }
}

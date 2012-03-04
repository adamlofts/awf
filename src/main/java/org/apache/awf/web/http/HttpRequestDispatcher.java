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

import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RequestDispatcher</code> is responsible for invoking the
 * appropriate <code>RequestHandler</code> method for the current
 * <code>HttpRequest</code>.
 */
public class HttpRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcher.class);

    public static void dispatch(RequestHandler rh, HttpRequest request, HttpResponse response) {
        if (rh != null) {
            HttpVerb method = request.getMethod();
            try {
                switch (method) {
                case GET:
                    rh.get(request, response);
                    break;
                case POST:
                    rh.post(request, response);
                    break;
                case HEAD:
                    rh.head(request, response);
                    break;
                case PUT:
                    rh.put(request, response);
                    break;
                case DELETE:
                    rh.delete(request, response);
                    break;
                case OPTIONS: // Fall through
                case TRACE:
                case CONNECT:
                default:
                    logger.warn("Unimplemented Http metod received: {}", method);
                    // TODO send "not supported page (501) back to client"
                }
            } catch (HttpException he) {
                response.setStatus(he.getStatus());
                response.write(he.getMessage());
                if (rh.isMethodAsynchronous(request.getMethod())) {
                    response.finish();
                }
            }
        }
    }
}

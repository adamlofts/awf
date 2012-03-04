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

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.awf.configuration.Configuration;
import org.apache.awf.util.HttpUtil;
import org.apache.awf.web.handler.BadRequestRequestHandler;
import org.apache.awf.web.handler.ForbiddenRequestHandler;
import org.apache.awf.web.handler.HttpContinueRequestHandler;
import org.apache.awf.web.handler.NotFoundRequestHandler;
import org.apache.awf.web.handler.RequestHandler;
import org.apache.awf.web.handler.RequestHandlerFactory;
import org.apache.awf.web.handler.StaticContentHandler;
import org.apache.awf.web.http.HttpRequest;

import com.google.common.collect.ImmutableMap;

public class Application {

    /**
     * "Normal/Absolute" (non group capturing) RequestHandlers e.g. "/",
     * "/persons"
     */
    private final ImmutableMap<String, RequestHandler> absoluteHandlers;

    /**
     * Group capturing RequestHandlers e.g. "/persons/([0-9]+)",
     * "/persons/(\\d{1,3})"
     */
    private final ImmutableMap<String, RequestHandler> capturingHandlers;

    /**
     * A mapping between group capturing RequestHandlers and their corresponding
     * pattern ( e.g. "([0-9]+)" )
     */
    private final ImmutableMap<RequestHandler, Pattern> patterns;

    /**
     * The directory where static content (files) will be served from.
     */
    private String staticContentDir;

    /**
     * A copy of the <code>Configuration</code> used to create this type.
     */
    private Configuration configuration;

    public Application(Map<String, RequestHandler> handlers) {
        ImmutableMap.Builder<String, RequestHandler> builder = new ImmutableMap.Builder<String, RequestHandler>();
        ImmutableMap.Builder<String, RequestHandler> capturingBuilder = new ImmutableMap.Builder<String, RequestHandler>();
        ImmutableMap.Builder<RequestHandler, Pattern> patternsBuilder = new ImmutableMap.Builder<RequestHandler, Pattern>();

        for (String path : handlers.keySet()) {
            int index = path.lastIndexOf("/");
            String group = path.substring(index + 1, path.length());
            if (containsCapturingGroup(group)) {
                // path ends with capturing group, e.g path ==
                // "/person/([0-9]+)"
                capturingBuilder.put(path.substring(0, index + 1), handlers.get(path));
                patternsBuilder.put(handlers.get(path), Pattern.compile(group));
            } else {
                // "normal" path, e.g. path == "/"
                builder.put(path, handlers.get(path));
            }
        }
        absoluteHandlers = builder.build();
        capturingHandlers = capturingBuilder.build();
        patterns = patternsBuilder.build();
    }

    /**
     * 
     * @param path Requested path
     * @return Returns the {@link RequestHandler} associated with the given
     *         path. If no mapping exists a {@link NotFoundRequestHandler} is
     *         returned.
     */
    private RequestHandler getHandler(String path) {

        RequestHandler rh = absoluteHandlers.get(path);
        if (rh == null) {
            rh = getCapturingHandler(path);
            if (rh == null) {
                rh = getStaticContentHandler(path);
                if (rh != null) {
                    return rh;
                }
            } else {
                return RequestHandlerFactory.cloneHandler(rh);
            }
        } else {
            return RequestHandlerFactory.cloneHandler(rh);
        }

        return NotFoundRequestHandler.getInstance();
    }

    public RequestHandler getHandler(HttpRequest request) {

        if (!HttpUtil.verifyRequest(request)) {
            return BadRequestRequestHandler.getInstance();
        }
        // if @Authenticated annotation is present, make sure that the
        // request/user is authenticated
        // (i.e RequestHandler.getCurrentUser() != null).
        RequestHandler rh = getHandler(request.getRequestedPath());
        if (rh.isMethodAuthenticated(request.getMethod()) && rh.getCurrentUser(request) == null) {
            return ForbiddenRequestHandler.getInstance();
        }

        if (request.expectContinue()) {
            return HttpContinueRequestHandler.getInstance();
        }

        return rh;
    }

    private boolean containsCapturingGroup(String group) {
        boolean containsGroup = group.matches("^\\(.*\\)$");
        Pattern.compile(group); // throws PatternSyntaxException if group is
        // malformed regular expression
        return containsGroup;
    }

    private RequestHandler getCapturingHandler(String path) {
        int index = path.lastIndexOf("/");
        if (index != -1) {
            String init = path.substring(0, index + 1); // path without its last
            // segment
            String group = path.substring(index + 1, path.length());
            RequestHandler handler = capturingHandlers.get(init);
            if (handler != null) {
                Pattern regex = patterns.get(handler);
                if (regex.matcher(group).matches()) {
                    return handler;
                }
            }
        }
        return null;
    }

    private RequestHandler getStaticContentHandler(String path) {
        if (staticContentDir == null || path.length() <= staticContentDir.length()) {
            return null; // quick reject (no static dir or simple contradiction)
        }

        if (path.substring(1).startsWith(staticContentDir)) {
            return StaticContentHandler.getInstance();
        } else {
            return null;
        }
    }

    void setStaticContentDir(String scd) {
        staticContentDir = scd;
    }

    /**
     * Set the <code>Configuration</code> for use with this type.
     * 
     * @param configuration the <code>Configuration</code> to apply.
     */
    public void setConfiguration(Configuration configuration) {

        this.configuration = configuration;
    }

    /**
     * Retrieve the <code>Configuration</code> used by this type.
     * 
     * @return the current <code>Configuration</code>.
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}

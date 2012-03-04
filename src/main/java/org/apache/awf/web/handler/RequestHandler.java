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
package org.apache.awf.web.handler;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.awf.annotation.Asynchronous;
import org.apache.awf.annotation.Authenticated;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.HttpResponse;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.apache.awf.web.http.protocol.HttpVerb;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public abstract class RequestHandler implements Cloneable {

    private final ImmutableMap<HttpVerb, Boolean> asynchVerbs;
    private final ImmutableMap<HttpVerb, Boolean> authVerbs;

    public RequestHandler() {

        Map<HttpVerb, Boolean> asyncV = Maps.newHashMap();
        Map<HttpVerb, Boolean> authV = Maps.newHashMap();
        for (HttpVerb verb : HttpVerb.values()) {
            asyncV.put(verb, isMethodAnnotated(verb, Asynchronous.class));
            authV.put(verb, isMethodAnnotated(verb, Authenticated.class));
        }

        asynchVerbs = ImmutableMap.copyOf(asyncV);
        authVerbs = ImmutableMap.copyOf(authV);
    }

    private boolean isMethodAnnotated(HttpVerb verb, Class<? extends Annotation> annotation) {
        try {
            Class<?>[] parameterTypes = { HttpRequest.class, HttpResponse.class };
            return getClass().getMethod(verb.toString().toLowerCase(), parameterTypes).getAnnotation(annotation) != null;
        } catch (NoSuchMethodException nsme) {
            return false;
        }
    }

    public boolean isMethodAsynchronous(HttpVerb verb) {
        return asynchVerbs.get(verb);
    }

    public boolean isMethodAuthenticated(HttpVerb verb) {
        return authVerbs.get(verb);
    }

    // Default implementation of HttpMethods return a 501 page
    public void get(HttpRequest request, HttpResponse response) {
        response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        response.write("");
    }

    public void post(HttpRequest request, HttpResponse response) {
        response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        response.write("");
    }

    public void put(HttpRequest request, HttpResponse response) {
        response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        response.write("");
    }

    public void delete(HttpRequest request, HttpResponse response) {
        response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        response.write("");
    }

    public void head(HttpRequest request, HttpResponse response) {
        response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
        response.write("");
    }

    public String getCurrentUser(HttpRequest request) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

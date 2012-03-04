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
package org.apache.awf.configuration;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.awf.web.HttpServer;
import org.apache.awf.web.handler.RequestHandler;

/**
 * Simple type to hold configuration options, to be passed to {@link HttpServer}
 * .
 */
public class Configuration {

    /**
     * The default directory for static content.
     */
    static final String DEFAULT_STATIC_DIRECTORY = "static";

    /**
     * The package under which <code>RequestHandler</code> implementations are
     * to be found, for example "org.apache.awf".
     */
    private String handlerPacakge;

    /**
     * The directory from which static content should be retrieved.
     */
    private String staticDirectory;

    /**
     * Indicates whether ETags should be generated and applied.
     */
    private boolean createETags;

    /**
     * Contains manually mapped handlers
     */
    private final Map<String, RequestHandler> handlerMap = Maps.newHashMap();


    /**
     * Retrieve a map containing manually defined map between request path pattern
     * and {@link RequestHandler}'s. if not null this map is used as default
     * handlers stack.
     *
     * @return the request handlers map.
     */
    public Map<String,RequestHandler> getHandlerMap(){
        return handlerMap;
    }

    /**
     * put a new path - handler mapping in the handler map.
     * @param path          path for which the handler should be used
     * @param handler       RequestHandler used to process HTTP request
     * @return              return this instance for chained calls.
     */
    public Configuration addHandler(String path, RequestHandler handler){
        handlerMap.put(path, handler);
        return this;
    }

    /**
     * Retrieve the package under which <code>RequestHandler</code>
     * implementations are to be found, for example "org.apache.awf".
     * 
     * @return the current package name.
     */
    public String getHandlerPackage() {
        return Strings.nullToEmpty(handlerPacakge).trim().isEmpty() ? "" : handlerPacakge;
    }

    /**
     * Set package under which <code>RequestHandler</code> implementations are
     * to be found, for example "org.apache.awf".
     * 
     * @param handlerPackage the name of the package.
     */
    public Configuration setHandlerPackage(String handlerPackage) {
        handlerPacakge = handlerPackage;
        return this;
    }

    /**
     * Retrieve directory from which static content should be retrieved.
     * Defaults to the value of {@link #DEFAULT_STATIC_DIRECTORY} where empty or
     * <code>null</code>.
     * 
     * @return the current name of the static directory.
     */
    public String getStaticDirectory() {
        return Strings.nullToEmpty(staticDirectory).trim().isEmpty() ? DEFAULT_STATIC_DIRECTORY : staticDirectory;
    }

    /**
     * Set the directory from which static content should be retrieved.
     * 
     * @param staticDirectory the directory name for use.
     */
    public Configuration setStaticDirectory(String staticDirectory) {
        this.staticDirectory = staticDirectory;
        return this;
    }

    /**
     * Determine whether ETags should currently be generated and applied.
     * 
     * @return <code>true</code> if they are to be created; <code>false</code>
     *         otherwise.
     */
    public boolean shouldCreateETags() {
        return createETags;
    }

    /**
     * Set whether ETags should be generated and applied.
     * 
     * @param createETags <code>true</code> to create them; <code>false</code>
     *            otherwise.
     */
    public Configuration setCreateETags(boolean createETags) {
        this.createETags = createETags;
        return this;
    }
}

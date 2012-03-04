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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.awf.annotation.Path;
import org.apache.awf.util.ReflectionTools;
import org.apache.awf.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Provides functionality to retrieve known <code>Annotation</code> types and
 * associated values.
 */
public class AnnotationsScanner {

    private final static Logger logger = LoggerFactory.getLogger(AnnotationsScanner.class);

    /**
     * A <code>Map</code> of <code>RequestHandler</code>s associated with
     * {@link Path}s.
     */
    private Map<String, RequestHandler> pathHandlers = new HashMap<String, RequestHandler>();

    /**
     * Recursively iterate the given package, and attempt to resolve all
     * annotated references for <code>RequestHandler</code> implementations.
     * 
     * @param handlerPackage the base package to scan, for example
     *            "org.apache.awf".
     * @return a <code>Map&lt;String, RequestHandler&gt;</code> of handlers,
     *         which may be empty but not <code>null</code>.
     */
    public Map<String, RequestHandler> findHandlers(String handlerPackage) {

        if (Strings.isNullOrEmpty(handlerPackage)) {
            logger.warn("No RequestHandler package defined");
            return pathHandlers;
        }

        List<Class<?>> classes = findClasses(handlerPackage);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Path.class)) {

                RequestHandler handler = (RequestHandler) ReflectionTools.createInstance(clazz.getCanonicalName());
                Path path = clazz.getAnnotation(Path.class);
                pathHandlers.put(path.value(), handler);

                logger.info("Added RequestHandler [" + clazz.getCanonicalName() + "] for Path [" + path.value() + "]");
            }
        }

        return pathHandlers;
    }

    /**
     * Recursively finds all classes available to the context
     * <code>ClassLoader</code> from the given package.
     * 
     * @param packageName the package from which to commence the scan.
     * @return A <code>List</code> of <code>Class</code> references.
     */
    private List<Class<?>> findClasses(String packageName) {

        List<Class<?>> classes = new ArrayList<Class<?>>();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            logger.error("Context ClassLoader was not available");
            return classes;
        }

        String path = packageName.replace('.', '/');
        try {
            List<File> directories = new ArrayList<File>();

            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                directories.add(new File(resource.getFile()));
            }

            for (File directory : directories) {
                classes.addAll(findClasses(directory, packageName));
            }
        } catch (IOException e) {
            logger.error("Exception accessing resources for [" + path + "]", e);
        }

        return classes;
    }

    /**
     * Recursively finds all class files available for the given package from
     * the passed directory.
     * 
     * @param packageName the package from which to commence the scan.
     * @return A <code>List</code> of <code>Class</code> references.
     */
    private List<Class<?>> findClasses(File directory, String packageName) {

        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (directory == null || !directory.exists()) {
            logger.error("Directory is null value or non-existent, [" + directory + "]");
            return classes;
        }

        for (File file : directory.listFiles()) {
            try {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.'
                            + file.getName().substring(0, file.getName().length() - 6)));
                }
            } catch (ClassNotFoundException e) {
                logger.error("ClassNotFoundException", e);
            }
        }

        return classes;
    }
}

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
package org.apache.awf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common functionality relating to the use of Reflection.
 */
public class ReflectionTools {

    private final static Logger logger = LoggerFactory.getLogger(ReflectionTools.class);

    /**
     * Prevent instantiation of this type.
     */
    private ReflectionTools() {
        // Do nothing.
    }

    /**
     * Create an instance of the given type.
     * 
     * @param fqcn the fully-qualified class name of the required type.
     * @return an <code>Object</code> of the requested type, or
     *         <code>null</code> on any problem.
     */
    public static Object createInstance(String fqcn) {

        Object instance = null;

        try {
            instance = Class.forName(fqcn).newInstance();
        } catch (InstantiationException e) {
            logger.error("InstantiationException", e);
        } catch (IllegalAccessException e) {
            logger.error("IllegalAccessException", e);
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException", e);
        }

        return instance;
    }
}

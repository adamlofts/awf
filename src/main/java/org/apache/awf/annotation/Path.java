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
package org.apache.awf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Defines the request path for which the annotated <code>RequestHandler</code>
 * method is applicable. The path is defined from root, and so for example to
 * associate a <code>RequestHandler</code> with the top-level directory
 * "images":
 * <p>
 * <code>&#64;Path(&quot;/images/&quot;)</code>
 * </p>
 * <p>
 * Values are a combination of paths and regular expressions as understood by
 * {@link Pattern}. For example:
 * </p>
 * <ul>
 * <li><code>&#64;Path("/path/([\\w]+)")</code> matches any word character after
 * the path such as "/path/123", "/path/abc" or "/path/12ab".
 * <li><code>&#64;Path("/matchThis")</code> matches on the path itself, that is
 * "http://host/matchThis".
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Path {

    String value();
}

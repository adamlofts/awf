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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.awf.annotation.Path;
import org.apache.awf.configuration.AnnotationsScanner;
import org.apache.awf.web.handler.RequestHandler;
import org.junit.Test;

/**
 * Test cases for {@link AnnotationsScanner}.
 */
public class AnnotationsScannerTest {
	
    @Test
    public void testFindHandlers() {

        AnnotationsScanner scanner = new AnnotationsScanner();
        Map<String, RequestHandler> handlers = scanner.findHandlers("org.apache.awf.configuration");

        assertEquals(1, handlers.size());
    }

    @Test
    public void testFindHandlersForBadPath() {

        AnnotationsScanner scanner = new AnnotationsScanner();
        Map<String, RequestHandler> handlers = scanner.findHandlers("org.does.not.exist");

        assertEquals(0, handlers.size());
    }

    @Test
    public void testFindHandlersForEmptyPath() {

        AnnotationsScanner scanner = new AnnotationsScanner();
        Map<String, RequestHandler> handlers = scanner.findHandlers(null);

        assertEquals(0, handlers.size());
    }
}

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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.apache.awf.util.DateUtil;
import org.junit.Test;

/**
 * Test cases for {@link DateUtil}.
 */
public class DateUtilTest {

    @Test
    public void testParseToMillisecondsForLong() {

        final long actual = DateUtil.parseToMilliseconds("123");
        assertEquals(123, actual);
    }

    @Test
    public void testParseToMillisecondsForRFC1123String() {

        final long actual = DateUtil.parseToMilliseconds("Sat, 20 Feb 2010 18:12:38 GMT");
        assertEquals(1266689558000L, actual);
    }

    @Test
    public void testParseToMillisecondsForInvalidString() {

        final long actual = DateUtil.parseToMilliseconds("INVALID");
        assertEquals(0, actual);
    }

    @Test
    public void testParseToRFC1123() {

        final String actual = DateUtil.parseToRFC1123(1266689558000L);
        assertEquals("Sat, 20 Feb 2010 18:12:38 GMT", actual);
    }

    @Test
    public void testGetDateAsString() {

        Date date = new Date(1266689558000L);
        String actual = DateUtil.getDateAsString(date);
        assertEquals("Sat, 20 Feb 2010 18:12:38 GMT", actual);
    }
}

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

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.awf.web.http.HttpRequest;

public class HttpUtil {

    /*
     * MessageDigest are not thread-safe and are expensive to create. Do it
     * lazily for each thread that need access to one.
     */
    private static final ThreadLocal<MessageDigest> md = new ThreadLocal<MessageDigest>();

    public static boolean verifyRequest(HttpRequest request) {
        String version = request.getVersion();
        boolean requestOk = true;
        if (version.equals("HTTP/1.1")) { // TODO might be optimized? Could do
            // version.endsWith("1"), or similar
            requestOk = request.getHeader("host") != null;
        }

        return requestOk;
    }

    public static String getEtag(byte[] bytes) {
        if (md.get() == null) {
            try {
                md.set(MessageDigest.getInstance("MD5"));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 cryptographic algorithm is not available.", e);
            }
        }
        byte[] digest = md.get().digest(bytes);
        BigInteger number = new BigInteger(1, digest);
        // prepend a '0' to get a proper MD5 hash
        return '0' + number.toString(16);

    }

    public static String getEtag(File file) {
        // TODO RS 101011 Implement if etag response header should be present
        // while static file serving.
        return "";
    }
}

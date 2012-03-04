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

import java.io.File;

import javax.activation.FileTypeMap;

import org.apache.awf.util.DateUtil;
import org.apache.awf.web.http.HttpException;
import org.apache.awf.web.http.HttpRequest;
import org.apache.awf.web.http.HttpResponse;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RequestHandler that serves static content (files) from a predefined
 * directory.
 * 
 * "Cache-Control: public" indicates that the response MAY be cached by any
 * cache, even if it would normally be non-cacheable or cacheable only within a
 * non- shared cache.
 */
public class StaticContentHandler extends RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(StaticContentHandler.class);

    private final static StaticContentHandler instance = new StaticContentHandler();

    private final FileTypeMap mimeTypeMap = FileTypeMap.getDefaultFileTypeMap();

    public static StaticContentHandler getInstance() {
        return instance;
    }

    /** {inheritDoc} */
    @Override
    public void get(HttpRequest request, HttpResponse response) {
        perform(request, response, true);
    }

    /** {inheritDoc} */
    @Override
    public void head(final HttpRequest request, final HttpResponse response) {
        perform(request, response, false);
    }

    /**
     * @param request the <code>HttpRequest</code>
     * @param response the <code>HttpResponse</code>
     * @param hasBody <code>true</code> to write the message body;
     *            <code>false</code> otherwise.
     */
    private void perform(final HttpRequest request, final HttpResponse response, boolean hasBody) {

        final String path = request.getRequestedPath();
        final File file = new File(path.substring(1)); // remove the leading '/'
        if (!file.exists()) {
            throw new HttpException(HttpStatus.CLIENT_ERROR_NOT_FOUND);
        } else if (!file.isFile()) {
            throw new HttpException(HttpStatus.CLIENT_ERROR_FORBIDDEN, path + "is not a file");
        }

        final long lastModified = file.lastModified();
        response.setHeader("Last-Modified", DateUtil.parseToRFC1123(lastModified));
        response.setHeader("Cache-Control", "public");
        String mimeType = mimeTypeMap.getContentType(file);
        if ("text/plain".equals(mimeType)) {
            mimeType += "; charset=utf-8";
        }
        response.setHeader("Content-Type", mimeType);
        final String ifModifiedSince = request.getHeader("If-Modified-Since");
        if (ifModifiedSince != null) {
            final long ims = DateUtil.parseToMilliseconds(ifModifiedSince);
            if (lastModified <= ims) {
                response.setStatus(HttpStatus.REDIRECTION_NOT_MODIFIED);
                logger.debug("not modified");
                return;
            }
        }

        if (hasBody) {
            response.write(file);
        }
    }
}

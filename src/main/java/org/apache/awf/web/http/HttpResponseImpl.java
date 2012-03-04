/*
 *  or more contributor license agreements.  See the NOTICE file
 *  Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.awf.web.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.apache.awf.util.Closeables;
import org.apache.awf.util.CookieUtil;
import org.apache.awf.util.DateUtil;
import org.apache.awf.util.HttpUtil;
import org.apache.awf.web.http.protocol.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.awf.web.http.HttpServerDescriptor.WRITE_BUFFER_SIZE;

public class HttpResponseImpl implements HttpResponse {

    private final static Logger logger = LoggerFactory.getLogger(HttpResponseImpl.class);

    private final HttpProtocol protocol;
    private final SelectionKey key;

    private HttpStatus status = HttpStatus.SUCCESS_OK;

    private final Map<String, String> headers = new HashMap<String, String>();
    private final Map<String, String> cookies = Maps.newHashMap();
    private boolean headersCreated = false;
    private DynamicByteBuffer responseData = DynamicByteBuffer.allocate(WRITE_BUFFER_SIZE);

    private boolean createETag;

    public HttpResponseImpl(HttpProtocol protocol, SelectionKey key, boolean keepAlive) {
        this.protocol = protocol;
        this.key = key;
        headers.put("Server", "Apache AWF/0.4.0-SNAPSHOT");
        headers.put("Date", DateUtil.getCurrentAsString());
        headers.put("Connection", keepAlive ? "Keep-Alive" : "Close");
    }

    @Override
    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    /** {@inheritDoc} */
    @Override
    public void setCreateETag(boolean create) {
        createETag = create;
    }

    @Override
    public void setHeader(String header, String value) {
        headers.put(header, value);
    }

    @Override
    public void setCookie(String name, String value) {
        setCookie(name, value, -1, null, null, false, false);
    }

    @Override
    public void setCookie(String name, String value, long expiration) {
        setCookie(name, value, expiration, null, null, false, false);
    }

    @Override
    public void setCookie(String name, String value, String domain) {
        setCookie(name, value, -1, domain, null, false, false);
    }

    @Override
    public void setCookie(String name, String value, String domain, String path) {
        setCookie(name, value, -1, domain, path, false, false);
    }

    @Override
    public void setCookie(String name, String value, long expiration, String domain) {
        setCookie(name, value, expiration, domain, null, false, false);
    }

    @Override
    public void setCookie(String name, String value, long expiration, String domain, String path) {
        setCookie(name, value, expiration, domain, path, false, false);
    }

    @Override
    public void setCookie(String name, String value, long expiration, String domain, String path, boolean secure,
            boolean httpOnly) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Cookie name is empty");
        }
        if (name.trim().startsWith("$")) {
            throw new IllegalArgumentException("Cookie name is not valid");
        }
        StringBuffer sb = new StringBuffer(name.trim() + "=" + Strings.nullToEmpty(value).trim() + "; ");
        if (CharMatcher.JAVA_ISO_CONTROL.countIn(sb) > 0) {
            throw new IllegalArgumentException("Invalid cookie " + name + ": " + value);
        }
        if (expiration >= 0) {
            if (expiration == 0) {
                sb.append("Expires=" + DateUtil.getDateAsString(new Date(0)) + "; ");
            } else {
                sb.append("Expires=" + CookieUtil.maxAgeToExpires(expiration) + "; ");
            }
        }
        if (!Strings.isNullOrEmpty(domain)) {
            sb.append("Domain=" + domain.trim() + "; ");
        }
        if (!Strings.isNullOrEmpty(path)) {
            sb.append("Path=" + path.trim() + "; ");
        }
        if (secure) {
            sb.append("Secure; ");
        }
        if (httpOnly) {
            sb.append("HttpOnly; ");
        }
        cookies.put(name, sb.toString());
    }

    @Override
    public void clearCookie(String name) {
        if (Strings.emptyToNull(name) != null) {
            setCookie(name, null, 0);
        }
    }

    @Override
    public HttpResponse write(String data) {
        return write(data.getBytes(Charsets.UTF_8));
    }

    @Override
    public HttpResponse write(byte[] data) {
        responseData.put(data);
        return this;
    }

    @Override
    public long flush() {
        if (!headersCreated) {
            String initial = createInitalLineAndHeaders();
            responseData.prepend(initial);
            headersCreated = true;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        responseData.flip(); // prepare for write
        try {
            channel.write(responseData.getByteBuffer());
        } catch (IOException e) {
            logger.error("ClosedChannelException during channel.write(): {}", e.getMessage());
            Closeables.closeQuietly(protocol.getIOLoop(), key.channel());
        }
        long bytesFlushed = responseData.position();
        if (protocol.getIOLoop().hasKeepAliveTimeout(channel)) {
            protocol.prolongKeepAliveTimeout(channel);
        }
        if (responseData.hasRemaining()) {
            responseData.compact(); // make room for more data be "read" in
            try {
                key.channel().register(key.selector(), SelectionKey.OP_WRITE); // TODO
                // RS
                // 110621,
                // use
                // IOLoop.updateHandler
            } catch (ClosedChannelException e) {
                logger.error("ClosedChannelException during flush(): {}", e.getMessage());
                Closeables.closeQuietly(protocol.getIOLoop(), key.channel());
            }
            key.attach(responseData);
        } else {
            responseData.clear();
        }
        return bytesFlushed;
    }

    @Override
    public long finish() {
        long bytesWritten = 0;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = null;

        if (key.attachment() instanceof MappedByteBuffer) {
            MappedByteBuffer mbb = (MappedByteBuffer) key.attachment();
            if (mbb.hasRemaining() && clientChannel.isOpen()) {
                try {
                    bytesWritten = clientChannel.write(mbb);
                } catch (IOException e) {
                    logger.warn("Could not write to channel: ", e.getMessage());
                    Closeables.closeQuietly(key.channel());
                }
            }
            buffer = mbb;
        } else {
            if (clientChannel.isOpen()) {
                if (!headersCreated) {
                    setEtagAndContentLength();
                }
                bytesWritten = flush();
            }
            // close (or register for read) if
            // (a) DBB is attached but all data is sent to wire (hasRemaining ==
            // false)
            // (b) no DBB is attached (never had to register for write)
            if (key.attachment() instanceof DynamicByteBuffer) {
                buffer = ((DynamicByteBuffer) key.attachment()).getByteBuffer();
            }
        }
        // Do Not Close the socket if there is more data to send or this is a CONTINUE
        if ((buffer != null && buffer.hasRemaining()) || HttpStatus.SUCCESS_CONTINUE.equals(status)) {
            return bytesWritten;
        }
        protocol.closeOrRegisterForRead(key);
        return bytesWritten;
    }

    private void setEtagAndContentLength() {

        if (createETag && responseData.position() > 0) {
            setHeader("Etag", HttpUtil.getEtag(responseData.array()));
        }
        setHeader("Content-Length", String.valueOf(responseData.position()));
    }

    private String createInitalLineAndHeaders() {
        StringBuilder sb = new StringBuilder(status.line());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey());
            sb.append(": ");
            sb.append(header.getValue());
            sb.append("\r\n");
        }
        for (String cookie : cookies.values()) {
            sb.append("Set-Cookie: " + cookie + "\r\n");
        }

        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Experimental support.
     */
    @Override
    public long write(File file) {
        // setHeader("Etag", HttpUtil.getEtag(file));
        setHeader("Content-Length", String.valueOf(file.length()));
        long bytesWritten = 0;
        flush(); // write initial line + headers

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            FileChannel fileChannel = in.getChannel();
            long sizeNeeded = fileChannel.size();
            bytesWritten = fileChannel.transferTo(0, sizeNeeded, ((SocketChannel) key.channel()));

            if (bytesWritten < sizeNeeded) {
                // Set channel Position to write rest of data from good starting
                // offset
                fileChannel.position(bytesWritten);
                key.attach(in);
            } else {
                // Only close channel when file is totally transferred to
                // SocketChannel
                com.google.common.io.Closeables.closeQuietly(in);
            }

        } catch (IOException e) {
            logger.error("Error writing (static file {}) to response: {}", file.getAbsolutePath(), e.getMessage());

            // If an exception occurs here we should ensure that file is closed
            com.google.common.io.Closeables.closeQuietly(in);
        }

        return bytesWritten;
    }
}

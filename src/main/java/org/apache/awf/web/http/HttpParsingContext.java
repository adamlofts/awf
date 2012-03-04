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
package org.apache.awf.web.http;

import java.nio.ByteBuffer;

/**
 * Context object holding data of the currently or last parser execution.
 * Used to maintain buffer position, last Token,
 */
public class HttpParsingContext {


    enum TokenType{
        REQUEST_LINE,
        REQUEST_METHOD,
        REQUEST_URI,
        HTTP_VERSION,
        HEADER_NAME,
        HEADER_VALUE,
        BODY,
        NO_CHUNK,
        CHUNK_OCTET,
        CHUNK;

    }
    boolean chunked;

    ByteBuffer buffer;

    TokenType currentType = TokenType.REQUEST_LINE;

    int skips = 0;

    StringBuilder tokenValue = new StringBuilder(255);

    boolean complete = false;

    int currentPointer = 0;

    String lastHeaderName = null;

    int chunkSize = 0;

    int incrementAndGetPointer(){
        currentPointer = buffer.get();
        return currentPointer;
    }

    public boolean tokenGreaterThan(int maxLen) {
        return tokenValue.length() > maxLen;
    }

    void setBuffer(ByteBuffer buffer){
        this.buffer = buffer;
    }

    boolean hasRemaining(){
        return buffer.hasRemaining();
    }

    void setBodyFound(){
        currentType = TokenType.BODY;
        tokenValue.delete(0, Integer.MAX_VALUE);
    }

    public boolean isbodyFound() {
        return TokenType.BODY.equals(currentType);
    }

    void clearTokenBuffer(){
        if (complete){ // Free buffer when last was complete
            tokenValue.delete(0, Integer.MAX_VALUE);
        }
    }


    void deleteFirstCharFromTokenBuffer(){
        tokenValue.deleteCharAt(0);
    }

    void appendChar(){
        tokenValue.append((char)currentPointer);
    }

    /**
     * Stores the token value and define the completeness
     */
    void storeIncompleteToken(){
        storeTokenValue(currentType, false);
    }

    void storeCompleteToken(TokenType type){
        storeTokenValue(type, true);
    }

    private void storeTokenValue(TokenType type, boolean _complete){

        currentType = type;
        complete = _complete;
    }

    String getTokenValue(){
        return tokenValue.toString();
    }

    public void persistHeaderName() {
        lastHeaderName = tokenValue.toString();
    }

    public String getLastHeaderName() {
        return lastHeaderName;
    }
}

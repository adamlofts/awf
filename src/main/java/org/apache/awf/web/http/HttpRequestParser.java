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

import org.apache.awf.io.buffer.DynamicByteBuffer;
import org.apache.awf.web.http.protocol.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Builds HttpRequest using a given ByteBuffer and already existing request object (unfinished).
 * Uses an HttpBufferedLexer to retrieve Http Tokens and the HttpParsingContext stored in the request
 * to maintain parsing state.
 */
public class HttpRequestParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequestParser.class);

    private final HttpBufferedLexer lexer;

    public HttpRequestParser(){
        lexer = new HttpBufferedLexer();
    }


    public HttpRequestImpl parseRequestBuffer(ByteBuffer buffer){

        return parseRequestBuffer(buffer, null);
    }

    /**
     * Parse the data in the given buffer as an Http request. It handles segmented buffer
     * when the given request is not null.
     *
     * @param buffer    ByteBuffer containing data to parse
     * @param result    null if it's a new request or the incomplete request
     * @return          new HttpRequestImpl if result is null representing a complete or incomplete request
     *                  on error, it will return a MalformedHttpRequest.
     */
	public HttpRequestImpl parseRequestBuffer(ByteBuffer buffer,HttpRequestImpl result){


		if (result == null){
            result = new HttpRequestImpl();
        }
        int status = 1;
        HttpParsingContext context = result.getContext();
        context.setBuffer(buffer);


        if(context.chunkSize > 0){
            status = pushChunkToBody(buffer, result,  context);
        }

        // while no errors and buffer not finished
        while ((status = lexer.nextToken(context)) > 0){
           switch (context.currentType){
               case REQUEST_METHOD: {
                   result.setMethod(HttpVerb.valueOf(context.getTokenValue()));break;
               }
               case REQUEST_URI:{
                   result.setURI(context.getTokenValue());
                   break;
               }
               case HTTP_VERSION:{
                   result.setVersion(context.getTokenValue());break;
               }
               case HEADER_NAME:{
                   context.persistHeaderName();break;
               }
               case HEADER_VALUE:{
                   result.pushToHeaders(context.getLastHeaderName(), context.getTokenValue());break;
               }
               case BODY:{
                   result.initKeepAlive();
                   // Copy body data to the request bodyBuffer
                   if (result.getContentLength() > 0){
                      pushRemainingToBody(context.buffer, result.getBodyBuffer(), result.getContentLength());
                      status = 0;
                   }else if (!context.chunked){
                       context.chunked =true;
                       context.currentType = HttpParsingContext.TokenType.CHUNK_OCTET;
                       result.buildChunkedBody();
                   }else { // BODY Found on chunked encoding so request done
                       status = 0;
                   }
                   break;
               }
               case CHUNK_OCTET:{
                   String [] parts = context.getTokenValue().split(";");
                   if (parts.length >0){
                       try {
                           context.chunkSize = Integer.parseInt(parts[0].trim(), 16);
                           if (context.chunkSize == 0){// Last Chunk gets 0
                                context.currentType = HttpParsingContext.TokenType.HEADER_NAME;
                           }else {
                                result.incrementChunkSize(context.chunkSize);
                                context.incrementAndGetPointer();
                                status = pushChunkToBody(buffer, result, context);
                           }
                       } catch (NumberFormatException e) {
                            // Error while reading size BadFormat :p
                           status = -1;
                       }
                   }
               }
           }
           if(status <= 0){
               break;
           }

        }

        // There was an error while parsing request
        if (status < 0){
            result = MalFormedHttpRequest.instance;
        }

        // release the context buffer
        context.setBuffer(null);
        buffer.clear();
        return result;
	}

    private int pushChunkToBody(ByteBuffer buffer, HttpRequestImpl result, HttpParsingContext context) {
        int size = (buffer.remaining() > context.chunkSize ? context.chunkSize : buffer.remaining());
        result.getBodyBuffer().put(buffer.array(), buffer.position(), size);
        context.chunkSize =  context.chunkSize - size;

        buffer.position(buffer.position()+size);
        // Chunk not complete we need more data
        if (context.chunkSize > 0){
            return 0;
        }
        return 1;
    }

    /**
     * Fill's the body buffer with the data retrieved from the given buffer starting
     * at buffer position and copying given size byte.<br/>
     * This will ensure that body buffer does not contain more than size byte.
     */
	private void pushRemainingToBody(ByteBuffer buffer, DynamicByteBuffer body, int size){
		// If buffer is empty or there is no clength then skip this
		if (size == 0 || !buffer.hasRemaining()){
			return;
		}

		if (body.position() + buffer.remaining() > size){
			body.put(buffer.array(),  buffer.position(), size - body.position());
		}
		else {
			body.put(buffer.array(),  buffer.position(), buffer.remaining());
		}
	}
}
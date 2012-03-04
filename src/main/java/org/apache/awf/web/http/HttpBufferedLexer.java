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

/**
 * Lexer class responsible for lexing an HTTP stream into tokens.
 * The tokens are composed of Method, URI, Protocol version, Header name and header value.
 *
 */
public class HttpBufferedLexer {


    static final int LF     = (int)'\n';
    static final int CR     = (int)'\r';
    static final int SP     = (int)' ';
    static final int TAB    = (int)'\t';
    static final int COLON  = (int)':';

    static final int LINE_MAX_SIZE = 500;


    /**
     * Use ' ' as separator and forbids CR / LF
     */
    static final StopChars SP_SEPARATOR = new StopChars() {

        public boolean isSeparator(int ptr) {
            return ptr == SP;
        }


        public boolean isForbidden(int ptr) {
            return ptr == CR || ptr == LF;
        }
    };

    /**
     * Use CR or LF as separator and forbids nothing
     */
    static final StopChars CRLF_SEPARATOR = new StopChars() {

        public boolean isSeparator(int ptr) {
            return ptr == CR || ptr == LF;
        }

        public boolean isForbidden(int ptr) {
            return false;
        }
    };

    /**
     * Use ':' as separator and forbids CR or LF
     */
    static final StopChars HEADER_NAME_SEPARATOR = new StopChars() {

        public boolean isSeparator(int ptr) {
            return ptr == COLON;
        }

        public boolean isForbidden(int ptr) {
            return ptr == CR || ptr == LF;
        }
    };

    static final int METHOD_LENGTH = 7;
    static final int URI_LENGTH = 255;
    static final int VERSION_LENGTH = 10;
    static final int HEADER_NAME_LENGTH = 30;
    static final int HEADER_VALUE_LENGTH = 300;

    private ErrorStatus status = ErrorStatus.OK;
	
	enum ErrorStatus {
		OK,
		TOO_LONG_REQUEST_LINE, 
		BAD_HEADER_NAME_FORMAT,
		BAD_REQUEST,
		
	}

    /**
     * Reads the next HTTP token from context buffer
     * @param context Context object holding parsing data
     * @return -1 on errors, 0 if not complete and 1 on success
     */
	public int nextToken(HttpParsingContext context){
        int res = -1;
        context.clearTokenBuffer(); // Clean the token buffer if we start a new token akka last token was complete

        switch (context.currentType){
            case REQUEST_LINE: { // read the first token of the request line METHOD
                if (skipWhiteSpaceAndLine(context)){
                    // Get method token
                    res = nextWord(context, HttpParsingContext.TokenType.REQUEST_METHOD, SP_SEPARATOR, METHOD_LENGTH);
                    
                }else{ // EOS reached with no data 
                    return 0;
                }
                break;
            }
            case REQUEST_METHOD:{
                // Get URI token
                res =  nextWord(context, HttpParsingContext.TokenType.REQUEST_URI, SP_SEPARATOR, URI_LENGTH);
                break;
            }
            case REQUEST_URI:{ // request version
                res = nextWord(context, HttpParsingContext.TokenType.HTTP_VERSION, CRLF_SEPARATOR, VERSION_LENGTH);
                break;
            }
            case HTTP_VERSION:{ // First header line
                context.skips = 0;
                if (!skipEndOfLine(context)){
                    res = nextWord(context, HttpParsingContext.TokenType.HEADER_NAME, HEADER_NAME_SEPARATOR, HEADER_NAME_LENGTH);
                }else {
                    context.setBodyFound();
                    res = 1;
                }
                break;
            }
            case HEADER_NAME:{ // header value
               res = nextWord(context, HttpParsingContext.TokenType.HEADER_VALUE, CRLF_SEPARATOR, HEADER_VALUE_LENGTH);
               break;
            }
            case HEADER_VALUE:{ // Might be a header value for multiline headers, a header name, or Body
                context.skips = 0;
                if (!skipEndOfLine(context)){
                    if (context.currentPointer == SP || context.currentPointer == TAB){
                        context.deleteFirstCharFromTokenBuffer(); // Don't keep the first whitespace character
                        res = nextWord(context, HttpParsingContext.TokenType.HEADER_VALUE,CRLF_SEPARATOR, HEADER_VALUE_LENGTH);
                    }else {
                        res = nextWord(context, HttpParsingContext.TokenType.HEADER_NAME, HEADER_NAME_SEPARATOR, HEADER_NAME_LENGTH);
                    }
                }else {
                    context.setBodyFound();
                    res = 1;
                }
                break;
            }
            case CHUNK_OCTET:{
                context.skips=0;
                skipEndOfLine(context);
                res = nextWord(context, HttpParsingContext.TokenType.CHUNK_OCTET, CRLF_SEPARATOR,HEADER_VALUE_LENGTH);
                break;
            }
            case NO_CHUNK: // No Chunk so nothing to do
            default:{ // If BODY or other nothing to do
                res = 0;
            }
        }
        return res;

    }

	
	public boolean skipWhiteSpaceAndLine(HttpParsingContext context){

		while(context.hasRemaining()){
			if (context.incrementAndGetPointer() != CR && context.currentPointer != LF && context.currentPointer != SP){
                context.appendChar();
				return true;
			}
		}
		return false;
	}

    /**
     *
     */
    public int nextWord(HttpParsingContext context, HttpParsingContext.TokenType type, StopChars stopChars, int maxLen){
        int currentChar = 0;

		while(context.hasRemaining()){
			currentChar = context.incrementAndGetPointer();
			if (stopChars.isForbidden(currentChar)){
				return -1; // Bad format Request should not contain this char at this point
			} else if (stopChars.isSeparator(currentChar)){
                if (context.tokenGreaterThan(maxLen)){
                    return -1; // Too long
                }
                context.storeCompleteToken(type);
                return 1;
            }
            context.appendChar();
		}
        // No errors but the token is not complete
        if (context.tokenGreaterThan(maxLen)){
            return -1; // Too long
        }
        context.storeIncompleteToken();
		return 0;
    }


    /**
     * Skips all end of line characters.
     * @return true if body was found starting
     */
    public boolean skipEndOfLine(HttpParsingContext context){

        while(context.hasRemaining()){

            if (context.incrementAndGetPointer() != CR && context.currentPointer != LF){
                context.appendChar();
                return false;
            }else if (context.skips >= 2){ // Here we got CRLFCRLF combination so rest is the body
                return true;
            }

            context.skips++;
        }

        return false;
    }


    /**
     * Defines the Stop characters to use (Separator and Forbidden)
     */
    private interface StopChars {

        /**
         * Tells wether this char is a separator endind the current Token under parsing
         */
        boolean isSeparator(int ptr);

        /**
         * Tells wether this char is forbidden or not. If forbidden then parsing will raise an error
         */
        boolean isForbidden(int ptr);

    }
	
}
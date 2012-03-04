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
package org.apache.awf.web.http.client;

import java.util.Map;

import com.google.common.collect.Maps;

public class Response {
	
	private final long requestTime;
	private String statusLine;
	private final Map<String, String> headers = Maps.newHashMap();
	private String body = "";
	
	public Response(long requestStarted) {
		requestTime = System.currentTimeMillis() - requestStarted;
	}
	
	public void setStatuLine(String statusLine) {
		this.statusLine = statusLine;
	}
	
	public String getStatusLine() {
		return statusLine;
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public String getHeader(String key) {
		return headers.get(key);
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public String getBody() {
		return body;
	}
	
	/**
	 * @return The total execution time of the request/response round trip.
	 */
	public long getRequestTime() {
		return requestTime;
	}
	
	@Override
	public String toString() {
		return "HttpResponse [body=" + body + ", headers=" + headers
				+ "\n, statusLine=" + statusLine + "]\n" + ", request time: " + requestTime +"ms";
	}

	void addChunk(String chunk) {
		body += chunk;
	}
	
}

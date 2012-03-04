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
package org.apache.awf.web;

import org.apache.awf.configuration.Configuration;
import org.apache.awf.io.IOLoop;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test cases for {@link HttpServer}.
 */
public class HttpServerTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPortInRange_low() {
        int port = 0;
        HttpServer server = createServer();
        server.listen(port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPortInRange_high() {
        int port = 65536;
        HttpServer server = createServer();
        server.listen(port);
    }

    @Test
    public void testPortInRange_ok() {
        int port = 8084;
        HttpServer server = createServer();
        server.listen(port);
    }

    @Test
     public void multiThreadServerStartStop(){
         int port = 8181;
         HttpServer server = createServer();
         server.bind(port);
         server.start(3);

         Assert.assertEquals(3, server.getIoLoops().size());
         for (IOLoop loop : server.getIoLoops()){
             Assert.assertTrue(loop.isRunning());
         }

         server.stop();
         for (IOLoop loop : server.getIoLoops()){
             Assert.assertFalse("Loop-"+loop.toString()+" should be stopped",loop.isRunning());
         }

     }



    private HttpServer createServer() {

        HttpServer server = new HttpServer(new Configuration());

        return server;
    }
}

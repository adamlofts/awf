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

/**
 * The generic interface a caller must implement to receive a result from an
 * async call
 */
public interface AsyncResult<T> {

    /**
     * The asynchronous call failed to complete normally.
     * 
     * @param caught failure encountered while executing an async operation
     */
    void onFailure(Throwable caught);

    /**
     * Called when an asynchronous call completes successfully.
     * 
     * @param result return value of the async call
     */
    void onSuccess(T result);

}

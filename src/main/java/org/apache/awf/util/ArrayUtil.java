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

public class ArrayUtil {

    // private static final List<String> EMPTY_STRING_LIST = Arrays.asList("");
    // private static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static String[] dropFromEndWhile(String[] array, String regex) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (!array[i].trim().equals("")) {
                String[] trimmedArray = new String[i + 1];
                System.arraycopy(array, 0, trimmedArray, 0, i + 1);
                return trimmedArray;
            }
        }
        return null;
        // { // alternative impl
        // List<String> list = new ArrayList<String>(Arrays.asList(array));
        // list.removeAll(EMPTY_STRING_LIST);
        // return list.toArray(EMPTY_STRING_ARRAY);
        // }
    }

}

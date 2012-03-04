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

/**
 * The Knuth Morris Pratt string searching algorithm (or KMP algorithm) searches
 * for occurrences of a "word" W within a main "text string" S by employing the
 * observation that when a mismatch occurs, the word itself embodies sufficient
 * information to determine where the next match could begin, thus bypassing
 * re-examination of previously matched characters.
 * 
 * The algorithm was conceived by Donald Knuth and Vaughan Pratt and
 * independently by James H. Morris in 1977, but the three published it jointly.
 * 
 */

public class KnuthMorrisPrattAlgorithm {

    /**
     * Search for pattern in data, [start, end). Returns -1 if no match is found
     * or if pattern is of length 0.
     */
    public static int indexOf(byte[] data, int start, int end, byte[] pattern) {
        if (pattern.length == 0) {
            return -1;
        }
        int[] failure = failure(pattern);

        int j = 0;

        for (int i = 0; i < end; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    private static int[] failure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}

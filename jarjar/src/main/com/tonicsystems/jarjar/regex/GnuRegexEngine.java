/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tonicsystems.jarjar.regex;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;

public class GnuRegexEngine implements RegexEngine
{
    public Pattern compile(String pattern) {
        try {
            return new GnuPattern(new RE(pattern));
        } catch (final REException e) {
            throw new IllegalArgumentException(e.getMessage()) {
                public Throwable getCause() {
                    return e;
                }
            };
        }
    }

    private static class GnuPattern
    implements Pattern
    {
        private final RE re;
        
        public GnuPattern(RE re) {
            this.re = re;
        }
            
        public String replaceAll(String value, String replace) {
            return re.substituteAll(value, replace, 0, RE.REG_NO_INTERPOLATE);
        }
        
        public boolean matches(String value) {
            return re.isMatch(value);
        }
        
        public int groupCount() {
            return re.getNumSubs();
        }
        
        public Matcher getMatcher(final String value) {
            final REMatch match = re.getMatch(value, 0);
            return new Matcher() {
                public boolean matches() {
                    return re.isMatch(value); // TODO?
                }
                public int start() {
                    return match.getStartIndex();
                }
                public int end() {
                    return match.getEndIndex();
                }
                public String group(int index) {
                    return match.toString(index);
                }
            };
        }
        
        public String toString() {
            return re.toString();
        }
    }
}

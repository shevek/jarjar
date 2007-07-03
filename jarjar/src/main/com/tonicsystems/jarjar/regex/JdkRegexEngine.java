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

public class JdkRegexEngine implements RegexEngine
{
    public Pattern compile(String pattern) {
        final java.util.regex.Pattern re = java.util.regex.Pattern.compile(pattern);
        return new Pattern() {
            public String replaceAll(String value, String replace) {
                replace = replace.replaceAll("([$\\\\])", "\\\\$0");
                return re.matcher(value).replaceAll(replace);
            }
            public boolean matches(String value) {
                return re.matcher(value).matches();
            }
            public int groupCount() {
                return re.matcher("foo").groupCount();
            }
            public Matcher getMatcher(String value) {
                final java.util.regex.Matcher match = re.matcher(value);
                return new Matcher() {
                    public boolean matches() {
                      return match.matches();
                    }
                    public int start() {
                        return match.start();
                    }
                    public int end() {
                        return match.end();
                    }
                    public String group(int index) {
                        return match.group(index);
                    }
                };
            }
            public String toString() {
                return re.pattern();
            }
        };
    }
}

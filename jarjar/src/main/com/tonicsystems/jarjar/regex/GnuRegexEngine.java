package com.tonicsystems.jarjar.regex;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;

public class GnuRegexEngine implements RegexEngine
{
    public Pattern compile(String pattern) {
        try {
            final RE re = new RE(pattern);
            return new Pattern() {
                public String replaceAll(String value, String replace) {
                    return re.substituteAll(value, replace, 0, RE.REG_NO_INTERPOLATE);
                }
                public boolean matches(String value) {
                    return re.isMatch(value);
                }
                public int groupCount() {
                    return re.getNumSubs();
                }
                public Matcher getMatcher(String value) {
                    return getMatcher(value, 0);
                }
                public Matcher getMatcher(String value, int index) {
                    final REMatch match = re.getMatch(value, index);
                    return new Matcher() {
                        public boolean find() {
                            return match != null;
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
            };
        } catch (final REException e) {
            throw new IllegalArgumentException(e.getMessage()) {
                public Throwable getCause() {
                    return e;
                }
            };
        }
    }
}

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
                return getMatcher(value, 0);
            }
            public Matcher getMatcher(String value, final int index) {
                final java.util.regex.Matcher match = re.matcher(value);
                return new Matcher() {
                    public boolean find() {
                        return match.find(index);
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

package com.tonicsystems.jarjar.regex;

public interface Pattern
{
    String replaceAll(String value, String replace);
    boolean matches(String value);
    int groupCount();
    Matcher getMatcher(String value);
    Matcher getMatcher(String value, int index);
}

package com.tonicsystems.jarjar.regex;

public interface Matcher
{
    boolean find();
    int start();
    int end();
    String group(int index);
}

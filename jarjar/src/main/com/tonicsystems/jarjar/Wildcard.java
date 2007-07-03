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

package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.regex.*;
import java.util.ArrayList;
import java.util.Arrays;

class Wildcard
{
    private static RegexEngine REGEX = new JdkRegexEngine();
    
    private static Pattern dstar = REGEX.compile("\\*\\*");
    private static Pattern star  = REGEX.compile("\\*");
    private static Pattern estar = REGEX.compile("\\+\\??\\)\\Z");

    private final Pattern pattern;
    private final int count;
    private final ArrayList parts = new ArrayList(16); // kept for debugging
    private final String[] strings;
    private final int[] refs;

    public Wildcard(String pattern, String result) {
        if (pattern.equals("**"))
            throw new IllegalArgumentException("'**' is not a valid pattern");
        if (!checkIdentifierChars(pattern, "/*"))
            throw new IllegalArgumentException("Not a valid package pattern: " + pattern);
        if (pattern.indexOf("***") >= 0)
            throw new IllegalArgumentException("The sequence '***' is invalid in a package pattern");
        
        String regex = pattern;
        regex = dstar.replaceAll(regex, "(.+?)");
        regex =  star.replaceAll(regex, "([^/]+)");
        regex = estar.replaceAll(regex, "*)");
        this.pattern = REGEX.compile("\\A" + regex + "\\Z");
        this.count = this.pattern.groupCount();

        // TODO: check for illegal characters
        char[] chars = result.toCharArray();
        int max = 0;
        for (int i = 0, mark = 0, state = 0, len = chars.length; i < len + 1; i++) {
            char ch = (i == len) ? '@' : chars[i];
            if (state == 0) {
                if (ch == '@') {
                    parts.add(new String(chars, mark, i - mark));
                    mark = i + 1;
                    state = 1;
                }
            } else {
                switch (ch) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                default:
                    if (i == mark)
                        throw new IllegalArgumentException("Backslash not followed by a digit");
                    int n = Integer.parseInt(new String(chars, mark, i - mark));
                    if (n > max)
                        max = n;
                    parts.add(new Integer(n));
                    mark = i--;
                    state = 0;
                }
            }
        }
        int size = parts.size();
        strings = new String[size];
        refs = new int[size];
        Arrays.fill(refs, -1);
        for (int i = 0; i < size; i++) {
            Object v = parts.get(i);
            if (v instanceof String) {
                strings[i] = ((String)v).replace('.', '/');
            } else {
                refs[i] = ((Integer)v).intValue();
            }
        }
        if (count < max)
            throw new IllegalArgumentException("Result includes impossible placeholder \"@" + max + "\": " + result);
        // System.err.println(this);
    }

    public boolean matches(String value) {
        return getMatcher(value) != null;
    }

    public String replace(String value) {
        Matcher matcher = getMatcher(value);
        if (matcher != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < strings.length; i++)
                sb.append((refs[i] >= 0) ? matcher.group(refs[i]) : strings[i]);
            return sb.toString();
        }
        return null;
    }

    private Matcher getMatcher(String value) {
        Matcher matcher = pattern.getMatcher(value);
        if (matcher.matches() && checkIdentifierChars(value, "/"))
            return matcher;
        return null;
    }

    private static boolean checkIdentifierChars(String expr, String extra) {
        for (int i = 0, len = expr.length(); i < len; i++) {
            char c = expr.charAt(i);
            if (extra.indexOf(c) >= 0)
                continue;
            if (!Character.isJavaIdentifierPart(c))
                return false;
        }
        return true;
    }

    public String toString() {
        return "Wildcard{pattern=" + pattern + ",parts=" + parts + "}";
    }
}

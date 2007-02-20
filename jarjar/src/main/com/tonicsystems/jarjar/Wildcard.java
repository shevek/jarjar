/*
  Jar Jar Links - A utility to repackage and embed Java libraries
  Copyright (C) 2004  Tonic Systems, Inc.

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; see the file COPYING.  if not, write to
  the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA 02111-1307 USA
*/

package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.regex.*;
import java.util.ArrayList;
import java.util.Arrays;

class Wildcard
{
    private static RegexEngine REGEX = new GnuRegexEngine();
    
    private static Pattern dots  = REGEX.compile("\\.");
    private static Pattern tilde = REGEX.compile("~");
    private static Pattern dstar = REGEX.compile("\\*\\*");
    private static Pattern star  = REGEX.compile("\\*");
    private static Pattern estar = REGEX.compile("\\+\\??\\)\\Z");

    private Pattern pattern;
    private int count;

    private ArrayList parts = new ArrayList(16); // kept for debugging
    private String[] strings;
    private int[] refs;

    public Wildcard(String pattern, String result) {
        if (pattern.equals("**"))
            throw new IllegalArgumentException("'**' is not a valid pattern");
        if (!checkIdentifierChars(pattern, ".*"))
            throw new IllegalArgumentException("Not a valid package pattern: " + pattern);
        if (pattern.indexOf("***") >= 0)
            throw new IllegalArgumentException("The sequence '***' is invalid in a package pattern");
        
        String regex = pattern;
        regex =  dots.replaceAll(regex, "~");
        regex = dstar.replaceAll(regex, "(.+?)");
        regex =  star.replaceAll(regex, "([^/]+)");
        regex = estar.replaceAll(regex, "*)");
        regex = tilde.replaceAll(regex, "/");
        this.pattern = REGEX.compile("\\A" + regex + "\\Z");
        this.count = this.pattern.groupCount();

        compileResult(result);
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

    private void compileResult(String value) {
        // TODO: check for illegal characters
        char[] chars = value.toCharArray();
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
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
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
            throw new IllegalArgumentException("Result includes impossible placeholder \"@" + max + "\": " + value);
    }

    public String toString() {
        return "Wildcard{pattern=" + pattern + ",parts=" + parts + "}";
    }
}

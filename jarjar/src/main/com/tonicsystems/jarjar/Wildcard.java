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
    private static RegexEngine REGEX = MyRegexEngine.getInstance();
    
    public static final int STYLE_DESC = 0;
    public static final int STYLE_IDENTIFIER = 1;

    private static Pattern dots  = REGEX.compile("\\.");
    private static Pattern tilde = REGEX.compile("~");
    private static Pattern dstar = REGEX.compile("\\*\\*");
    private static Pattern star  = REGEX.compile("\\*");

    private Pattern descPattern;
    private Pattern identifierPattern;
    private int count;

    private ArrayList parts = new ArrayList(16); // kept for debugging
    private String[] strings;
    private int[] refs;

    public Wildcard(String pattern, String result) {
        compilePattern(pattern);
        compileResult(result);
        System.err.println(this);
    }

    public boolean matches(String value, int style) {
        return getPattern(style).matches(value);
    }

    public String replace(String value, int style) {
        Matcher matcher = getPattern(style).getMatcher(value);
        return (matcher.matches()) ? replace(value, style, matcher) : value;
    }

    private Pattern getPattern(int style) {
        switch (style) {
        case STYLE_DESC:
            return descPattern;
        case STYLE_IDENTIFIER:
            return identifierPattern;
        default:
            throw new IllegalArgumentException("Unknown style " + style);
        }
    }

    private String replace(String value, int style, Matcher match) {
        StringBuffer sb = new StringBuffer();
        sb.append(value.substring(0, match.start()));
        sb.append(match.group(1));
        for (int i = 0; i < strings.length; i++) {
            int ref = refs[i];
            if (ref == 0) {
                String s = match.group(0);
                s = s.substring(match.group(1).length(),
                                s.length() - match.group(count).length());
                sb.append(postProcess(s, style));
            } else if (ref > 0) {
                sb.append(postProcess(match.group(ref + 1), style));
            } else {
                sb.append(postProcess(strings[i], style));
            }
        }
        sb.append(match.group(count));
        sb.append(value.substring(match.end()));
        return sb.toString();
    }

    private String postProcess(String value, int style) {
        if (style == STYLE_IDENTIFIER) {
            value = value.replace('/', '.');
        } else {
            value = value.replace('.', '/');
        }
        return value;
    }

    private void compilePattern(String expr) {
        if (expr.equals("**"))
            throw new IllegalArgumentException("'**' is not a valid pattern");
        for (int i = 0, len = expr.length(); i < len; i++) {
            char ch = expr.charAt(i);
            switch (ch) {
            case '*':
            case '.':
                break;
            default:
                if (!Character.isJavaIdentifierPart(ch)) {
                    throw new IllegalArgumentException("Not a valid package pattern: " + expr);
                }
            }
        }
        if (expr.indexOf("***") >= 0) {
            throw new IllegalArgumentException("The sequence '***' is invalid in a package pattern");
        }

        String p1 = expr;
        p1 = dots.replaceAll(p1, "~");
        p1 = dstar.replaceAll(p1, "(.+?)");
        p1 = star.replaceAll(p1, "([^/]+?)");

        String p2 = p1;
        p2 = tilde.replaceAll(p2, "\\.");
        p1 = tilde.replaceAll(p1, "/");
        p1 = "(\\[*L)" + p1 + "(;)";   // TODO: optional semicolon?

        p1 = "\\A" + p1 + "\\Z";
        p2 = "\\A()" + p2 + "()\\b\\Z";
        // p2 = "\\b(L?)" + p2 + "()\\b";

        descPattern = REGEX.compile(p1);
        identifierPattern = REGEX.compile(p2);

        count = descPattern.groupCount();
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
                strings[i] = (String)v;
            } else {
                refs[i] = ((Integer)v).intValue();
            }
        }
        if (count < max)
            throw new IllegalArgumentException("Result includes impossible placeholder \"@" + max + "\": " + value);
    }

    public String toString() {
        return "Wildcard{descPattern=" + descPattern +
            ",identifierPattern=" + identifierPattern +
            ",parts=" + parts + "}";
    }
}

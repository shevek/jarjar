package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.regex.*;
import java.util.ArrayList;
import java.util.Arrays;

class Wildcard
{
    private static RegexEngine REGEX;

    static {
        try {
            REGEX = new GnuRegexEngine();
        } finally {
            if (REGEX == null)
                REGEX = new JdkRegexEngine();
        }
    }
    
    public static final int STYLE_DESC = 0;
    public static final int STYLE_DESC_ANYWHERE = 1;
    public static final int STYLE_IDENTIFIER = 2;

    private static Pattern dots  = REGEX.compile("\\.");
    private static Pattern tilde = REGEX.compile("~");
    private static Pattern dstar = REGEX.compile("\\*\\*");
    private static Pattern star  = REGEX.compile("\\*");

    private Pattern pattern1;
    private Pattern pattern2;
    private Pattern pattern3;
    private int count;

    private ArrayList parts = new ArrayList(16); // kept for debugging
    private String[] strings;
    private int[] refs;

    public Wildcard(String pattern, String result) {
        compilePattern(pattern);
        compileResult(result);
        // System.err.println(this);
    }

    public boolean matches(String value, int style) {
        return getPattern(style).matches(value);
    }

    public String replace(String value, int style) {
        Matcher matcher = getPattern(style).getMatcher(value);
        return (matcher.find()) ? replace(value, style, matcher) : value;
    }

    public String replaceAll(String value, int style) {
        String orig = value;
        Pattern p = getPattern(style);
        int count = 0;
        int index = 0;
        while (index < value.length()) {
            if (count++ > 50)
                throw new RuntimeException("Infinite loop detected processing \"" + orig + "\"");
            Matcher matcher = p.getMatcher(value, index);
            if (matcher.find()) {
                value = replace(value, style, matcher);
                index = matcher.end();
            } else {
                break;
            }
        }
        return value;
    }

    private Pattern getPattern(int style) {
        switch (style) {
        case STYLE_DESC:
            return pattern1;
        case STYLE_DESC_ANYWHERE:
            return pattern2;
        case STYLE_IDENTIFIER:
            return pattern3;
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

        String p3 = p1;
        p3 = tilde.replaceAll(p3, "\\.");
        p1 = tilde.replaceAll(p1, "/");
        p1 = "(\\[*L)" + p1 + "(;)";   // TODO: optional semicolon?

        String p2 = p1;
        p1 = "\\A" + p1 + "\\Z";
        p3 = "\\b(L?)" + p3 + "()\\b";

        pattern1 = REGEX.compile(p1);
        pattern2 = REGEX.compile(p2);
        pattern3 = REGEX.compile(p3);

        count = pattern1.groupCount();
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
        return "Wildcard{pattern1=" + pattern1 +
            ",pattern2=" + pattern2 +
            ",pattern3=" + pattern3 +
            ",parts=" + parts + "}";
    }
}

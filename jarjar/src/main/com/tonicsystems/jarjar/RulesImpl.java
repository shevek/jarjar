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

import java.util.*;
import org.objectweb.asm.Attribute;

class RulesImpl implements Rules
{
    private static final String RESOURCE_SUFFIX = "RESOURCE";
    
    private Wildcard[] wildcards;
    private HashMap cache = new HashMap();
    private boolean verbose;

    public RulesImpl(List ruleList, boolean verbose) {
        this.verbose = verbose;
        wildcards = PatternElement.createWildcards(ruleList);
    }

    public String fixPath(String path) {
        int slash = path.lastIndexOf('/');
        String end;
        if (slash < 0) {
            end = path;
            path = RESOURCE_SUFFIX;
        } else {
            end = path.substring(slash + 1);
            path = path.substring(0, slash + 1) + RESOURCE_SUFFIX;
        }
        path = fixName(path);
        path = path.substring(0, path.length() - RESOURCE_SUFFIX.length()) + end;
        return path;
    }
    
    public String fixDesc(String desc) {
        if (desc.charAt(desc.length() - 1) != ';')
            return desc;
        String value = (String)cache.get(desc);
        if (value == null) {
            value = replaceHelper(desc, Wildcard.STYLE_DESC);
            cache.put(desc, value);
        }
        return value;
    }

    private String replaceHelper(String value, int style) {
        for (int i = 0; i < wildcards.length; i++) {
            String test = wildcards[i].replace(value, style);
            if (!test.equals(value))
                return test;
        }
        return value;
    }

    public String fixName(String name) {
        if (name == null)
            return null;
        String desc = fixDesc("L" + name + ";");
        return desc.substring(1, desc.length() - 1);
    }

    public String fixMethodDesc(String desc) {
        String value = (String)cache.get(desc);
        if (value == null) {
            if (desc.indexOf('L') < 0) {
                value = desc;
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append('(');
                int end = desc.lastIndexOf(')');
                for (int i = 1; i < end; i++) {
                    char c = desc.charAt(i);
                    if (c == 'L') {
                        for (int j = i + 1; j < end; j++) {
                            if (desc.charAt(j) == ';') {
                                sb.append(fixDesc(desc.substring(i, j + 1)));
                                i = j;
                                break;
                            }
                        }
                    } else {
                        sb.append(c);
                    }
                }
                sb.append(')');
                sb.append(fixDesc(desc.substring(end + 1)));
                value = sb.toString();
            }
            cache.put(desc, value);
        }
        return value;
    }

    public String fixString(String className, String value) {
        String newValue = fixPath(value);
        if (newValue.equals(value))
            newValue = replaceHelper(newValue, Wildcard.STYLE_IDENTIFIER);
        if (verbose) {
            if (newValue.equals(value)) {
                // System.err.println("Kept -> " + className + " \"" + value + "\"");
            } else {
                System.err.println("Changed " + className + " \"" + value + "\" -> \"" + newValue + "\"");
            }
        }
        return newValue;
    }

    public Attribute fixAttribute(Attribute attrs) {
        // TODO?
        return attrs;
    }
}

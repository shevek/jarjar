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
import com.tonicsystems.jarjar.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;
import org.objectweb.asm.commons.*;
import java.util.*;

class PackageRemapper extends Remapper
{
    private static final String RESOURCE_SUFFIX = "RESOURCE";
    private static final Pattern IS_DESC = new GnuRegexEngine().compile("\\[*([VZCBSIFJD]|L[\\w\\.]+;)");
    
    private final Wildcard[] wildcards;
    private final HashMap typeCache = new HashMap();
    private final HashMap pathCache = new HashMap();
    private final HashMap valueCache = new HashMap();
    private final HashMap cache = new HashMap();
    private final boolean verbose;

    public PackageRemapper(List ruleList, boolean verbose) {
        this.verbose = verbose;
        wildcards = PatternElement.createWildcards(ruleList);
    }

    protected String map(String key) {
        String s = (String)typeCache.get(key);
        if (s == null) {
            s = replaceHelper(key, Wildcard.STYLE_INTERNAL);
            if (key.equals(s))
                s = null;
            typeCache.put(key, s);
        }
        return s;
    }

    public String mapPath(String path) {
        String s = (String)pathCache.get(path);
        if (s == null) {
            s = path;
            int slash = s.lastIndexOf('/');
            String end;
            if (slash < 0) {
                end = s;
                s = RESOURCE_SUFFIX;
            } else {
                end = s.substring(slash + 1);
                s = s.substring(0, slash + 1) + RESOURCE_SUFFIX;
            }
            boolean absolute = s.startsWith("/");
            if (absolute)
                s = s.substring(1);
            String after = mapType(s);
            s = after;
            if (absolute)
                s = "/" + s;
            s = s.substring(0, s.length() - RESOURCE_SUFFIX.length()) + end;
            pathCache.put(path, s);
        }
        return s;
    }

    public Object mapValue(Object value) {
        if (value instanceof String) {
            String s = (String)valueCache.get(value);
            if (s == null) {
                s = fixClassForName((String)value);
                if (s.equals(value))
                    s = mapPath(s);
                if (s.equals(value))
                    s = replaceHelper(s, Wildcard.STYLE_IDENTIFIER);
                valueCache.put(value, s);
            }
            // TODO: add back class name to verbose message
            if (verbose && !s.equals(value))
                System.err.println("Changed \"" + value + "\" -> \"" + s + "\"");
            return s;
        } else {
            return super.mapValue(value);
        }
    }

    private String fixClassForName(String value)
    {
        if (value.indexOf('.') >= 0) {
            String desc1 = value.replace('.', '/');
            if (IS_DESC.matches(desc1)) {
                try {
                    String desc2 = mapDesc(desc1);
                    if (!desc2.equals(desc1))
                        return desc2.replace('/', '.');
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace(System.err);
                }
            }
        }
        return value;
    }
            
    private String replaceHelper(String value, int style) {
        for (int i = 0; i < wildcards.length; i++) {
            String test = wildcards[i].replace(value, style);
            if (test != null)
                return test;
        }
        return value;
    }
}

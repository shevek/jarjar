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

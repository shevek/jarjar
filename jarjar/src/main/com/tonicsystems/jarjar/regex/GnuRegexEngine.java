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

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;

public class GnuRegexEngine implements RegexEngine
{
    public Pattern compile(String pattern) {
        try {
            return new GnuPattern(new RE(pattern));
        } catch (final REException e) {
            throw new IllegalArgumentException(e.getMessage()) {
                public Throwable getCause() {
                    return e;
                }
            };
        }
    }

    private static class GnuPattern
    implements Pattern
    {
        private final RE re;
        
        public GnuPattern(RE re) {
            this.re = re;
        }
            
        public String replaceAll(String value, String replace) {
            return re.substituteAll(value, replace, 0, RE.REG_NO_INTERPOLATE);
        }
        
        public boolean matches(String value) {
            return re.isMatch(value);
        }
        
        public int groupCount() {
            return re.getNumSubs();
        }
        
        public Matcher getMatcher(final String value) {
            final REMatch match = re.getMatch(value, 0);
            return new Matcher() {
                public boolean matches() {
                    return re.isMatch(value); // TODO?
                }
                public int start() {
                    return match.getStartIndex();
                }
                public int end() {
                    return match.getEndIndex();
                }
                public String group(int index) {
                    return match.toString(index);
                }
            };
        }
        
        public String toString() {
            return re.toString();
        }
    }
}

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

import java.io.*;
import java.util.*;

abstract public class AbstractDepHandler implements DepHandler
{
    protected int level;
    private Set seenIt = new HashSet();
    
    protected AbstractDepHandler(int level) {
        this.level = level;
    }
    
    public void handle(PathClass from, PathClass to) throws IOException {
        Pair pair;
        if (level == LEVEL_JAR) {
            pair = new Pair(from.getClassPath(), to.getClassPath());
        } else {
            pair = new Pair(from.getClassName(), to.getClassName());
        }
        if (!seenIt.contains(pair)) {
            seenIt.add(pair);
            handle((String)pair.getObject1(), (String)pair.getObject2());
        }
    }

    private static int commonPrefixLength(String s1, String s2) {
        for (int i = 0, len = Math.min(s1.length(), s2.length()); i < len; i++) {
            if (s1.charAt(i) != s2.charAt(i))
                return i;
        }
        return s1.length();
    }
    
    abstract protected void handle(String from, String to) throws IOException;

    public void handleStart() throws IOException { }
    public void handleEnd() throws IOException { }
}



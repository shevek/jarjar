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

class RulesFileParser
{
    private RulesFileParser() {
    }

    public static List parse(File file) throws IOException {
        return parse(new FileReader(file));
    }

    public static List parse(String value) throws IOException {
        return parse(new java.io.StringReader(value));
    }

    private static List parse(Reader r) throws IOException {
        List patterns = new ArrayList();
        BufferedReader br = new BufferedReader(r);
        int c = 1;
        String line;
        while ((line = br.readLine()) != null) {
            List parts = splitOnWhitespace(line);
            if (parts.size() < 2)
                error(c, parts);
            String type = (String)parts.get(0);
            PatternElement element = null;
            if (type.equals("rule")) {
                if (parts.size() < 3)
                    error(c, parts);
                element = new Rule();
                ((Rule)element).setResult((String)parts.get(2));
            } else if (type.equals("zap")) {
                element = new Zap();
            } else if (type.equals("keep")) {
                element = new Keep();
            } else if (type.equals("kill") || type.equals("depkill")) {
                element = new Kill();
            } else {
                error(c, parts);
            }
            element.setPattern((String)parts.get(1));
            patterns.add(element);
            c++;
        }
        r.close();
        return patterns;
    }

    private static void error(int line, List parts) {
        throw new IllegalArgumentException("Error on line " + line + ": " + parts);
    }

    private static List splitOnWhitespace(String line) {
        List list = new ArrayList();
        Enumeration e = new StringTokenizer(line);
        while (e.hasMoreElements())
            list.add(e.nextElement());
        return list;
    }
}

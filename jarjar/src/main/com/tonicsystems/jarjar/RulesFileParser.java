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
import java.io.*;
import java.util.*;

class RulesFileParser
{
    private static RegexEngine REGEX = MyRegexEngine.getInstance();
    private static Pattern WS = REGEX.compile("\\s+");

    private List ruleList;
    private List zapList;
    private List killList;

    public void parse(File file) throws IOException {
        parse(new FileReader(file));
    }

    public void parse(String value) throws IOException {
        parse(new StringReader(value));
    }

    public List getRuleList() {
        return ruleList;
    }

    public List getZapList() {
        return zapList;
    }

    private void parse(Reader r) throws IOException {
        ruleList = new ArrayList();
        zapList = new ArrayList();
        killList = new ArrayList();
        BufferedReader br = new BufferedReader(r);
        int c = 1;
        String line;
        while ((line = br.readLine()) != null) {
            List parts = RegexUtils.split(WS, line);
            if (parts.size() < 2)
                error(c, parts);
            String type = (String)parts.get(0);
            PatternElement element = null;
            if (type.equals("rule")) {
                if (parts.size() < 3)
                    error(c, parts);
                ruleList.add(element = new Rule());
                ((Rule)element).setResult((String)parts.get(2));
            } else if (type.equals("zap")) {
                zapList.add(element = new Zap());
            } else if (type.equals("depkill")) {
                killList.add(element = new DepKill());
            } else {
                error(c, parts);
            }
            element.setPattern((String)parts.get(1));
            c++;
        }
        r.close();
    }

    private static void error(int line, List parts) {
        throw new IllegalArgumentException("Error on line " + line + ": " + parts);
    }
}

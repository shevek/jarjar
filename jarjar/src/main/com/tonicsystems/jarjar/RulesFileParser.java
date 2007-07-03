/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

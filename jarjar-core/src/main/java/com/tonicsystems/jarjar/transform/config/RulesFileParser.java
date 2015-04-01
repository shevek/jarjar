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
package com.tonicsystems.jarjar.transform.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.annotation.Nonnull;

public class RulesFileParser {

    private RulesFileParser() {
    }

    @Nonnull
    public static List<PatternElement> parse(@Nonnull File file) throws IOException {
        return parse(new FileReader(file));
    }

    @Nonnull
    public static List<PatternElement> parse(@Nonnull String value) throws IOException {
        return parse(new java.io.StringReader(value));
    }

    @Nonnull
    private static List<String> split(@Nonnull String line) {
        StringTokenizer tok = new StringTokenizer(line);
        List<String> out = new ArrayList<String>();
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.startsWith("#"))
                break;
            out.add(token);
        }
        return out;
    }

    @Nonnull
    private static List<PatternElement> parse(@Nonnull Reader r) throws IOException {
        try {
            List<PatternElement> patterns = new ArrayList<PatternElement>();
            BufferedReader br = new BufferedReader(r);
            int c = 1;
            String line;
            while ((line = br.readLine()) != null) {
                List<String> words = split(line);
                if (words.isEmpty())
                    continue;
                if (words.size() < 2)
                    throw error(c, words, "not enough words on line.");
                String type = words.get(0);
                PatternElement element;
                if (type.equals("rule")) {
                    if (words.size() < 3)
                        throw error(c, words, "'rule' requires 2 arguments.");
                    Rule rule = new Rule();
                    rule.setResult(words.get(2));
                    element = rule;
                } else if (type.equals("zap")) {
                    element = new Zap();
                } else if (type.equals("keep")) {
                    element = new Keep();
                } else {
                    throw error(c, words, "Unrecognized keyword " + type);
                }
                element.setPattern(words.get(1));
                patterns.add(element);
                c++;
            }
            return patterns;
        } finally {
            r.close();
        }
    }

    @Nonnull
    private static IllegalArgumentException error(int line, List<String> words, String reason) {
        throw new IllegalArgumentException("Error on line " + line + ": " + words + ": " + reason);
    }
}

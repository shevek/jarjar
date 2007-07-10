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

import com.tonicsystems.jarjar.util.*;
import java.io.*;
import java.util.*;

public class Main {

  private static final String HELP;

  static {
    try {
      HELP = IoUtils.readIntoString(Main.class.getResourceAsStream("help.txt"));
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  private boolean verbose;
  private List patterns;
  private int level = DepHandler.LEVEL_CLASS;

  public static void main(String[] args) throws Exception {
    IoUtils.runMain(new Main(), args, "help");
  }

  public void help() {
    System.err.print(HELP);
  }

  public void strings(String cp) throws IOException {
    if (cp == null) {
      throw new IllegalArgumentException("cp is required");
    }
    new StringDumper().run(cp, new PrintWriter(System.out));
  }

  // TODO: make level an enum
  public void find(String level, String cp1, String cp2) throws IOException {
    if (level == null || cp1 == null) {
      throw new IllegalArgumentException("level and cp1 are required");
    }
    if (cp2 == null) {
      cp2 = cp1;
    }
    int levelFlag;
    if ("class".equals(level)) {
      levelFlag = DepHandler.LEVEL_CLASS;
    } else if ("jar".equals(level)) {
      levelFlag = DepHandler.LEVEL_JAR;
    } else {
      throw new IllegalArgumentException("unknown level " + level);
    }
    PrintWriter w = new PrintWriter(System.out);
    DepHandler handler = new TextDepHandler(w, levelFlag);
    new DepFind().run(cp1, cp2, handler);
    w.flush();
  }

  public void process(File rulesFile, File inJar, File outJar) throws IOException {
    if (rulesFile == null || inJar == null || outJar == null) {
      throw new IllegalArgumentException("rulesFile, inJar, and outJar are required");
    }
    boolean verbose = false; // TODO
    List rules = RulesFileParser.parse(rulesFile);
    MainProcessor proc = new MainProcessor(rules, verbose, true);
    StandaloneJarProcessor.run(inJar, outJar, proc);
    proc.strip(outJar);
  }
}

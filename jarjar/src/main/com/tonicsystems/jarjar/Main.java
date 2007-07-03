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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Main {

  public static final int STYLE_SIMPLE = 0;
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
    private int style = STYLE_SIMPLE;

    public static void main(String[] args) throws Exception {
      Main main = new Main();
      if (args.length > 0) {
        String command = args[0];
        Method[] methods = main.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
          Method method = methods[i];
          if (method.getName().equals(command)) {
            String[] remaining = new String[args.length - 1];
            System.arraycopy(args, 1, remaining, 0, remaining.length);
            try {
              method.invoke(main, (Object[]) bindParameters(method, remaining));
            } catch (InvocationTargetException e) {
              Throwable cause = e.getCause();
              if (cause instanceof IllegalArgumentException) {
                System.err.println("Syntax error: " + cause.getMessage());
              } else if (cause instanceof Exception) {
                throw (Exception) cause;
              } else {
                throw e;
              }
            }
            return;
          }
        }
      }
      main.help();
    }

  private static Object[] bindParameters(Method method, String[] args) {
    List parameters = new ArrayList();
    Class[] parameterTypes = method.getParameterTypes();
    for (int i = 0, len = parameterTypes.length; i < len; i++) {
      Class type = parameterTypes[i];
      int remaining = Math.max(0, args.length - i);
      if (type.equals(String[].class)) {
        String[] rest = new String[remaining];
        System.arraycopy(args, 1, rest, 0, remaining);
        parameters.add(rest);
      } else if (remaining > 0) {
        parameters.add(convertParameter(args[i], parameterTypes[i]));
      } else {
        parameters.add(null);
      }
    }
    return parameters.toArray();
  }

  private static Object convertParameter(String arg, Class type) {
    if (type.equals(String.class)) {
      return arg;
    } else if (type.equals(Integer.class)) {
      return Integer.valueOf(arg, 10);
    } else if (type.equals(File.class)) {
      return new File(arg);
    } else {
      throw new UnsupportedOperationException("Unknown type " + type);
    }
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

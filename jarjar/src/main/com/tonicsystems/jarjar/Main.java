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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.*;
import java.util.*;

public class Main
{
    public static final int STYLE_SIMPLE = 0;
    private static final String HELP;

    static {
        try {
            HELP = IoUtils.readIntoString(Main.class.getResourceAsStream("help.txt"));
        } catch (IOException e) {
            throw new NestedException(e);
        }
    }

    private boolean verbose;
    private List patterns;
    private int level = DepHandler.LEVEL_CLASS;
    private int style = STYLE_SIMPLE;

    // TODO: standalone kill?
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            help();
            return;
        }

        Getopt g = new Getopt("jarjar", args, ":fhvs", new LongOpt[]{
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'), 
            new LongOpt("rules", LongOpt.REQUIRED_ARGUMENT, null, 2),
            new LongOpt("find", LongOpt.NO_ARGUMENT, null, 'f'),
            new LongOpt("strings", LongOpt.NO_ARGUMENT, null, 's'),
            new LongOpt("level", LongOpt.REQUIRED_ARGUMENT, null, 3),
            new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, 4),
        });

        boolean find = false;
        boolean strings = false;
        try {
            Main main = new Main();
            int c;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                case 2:
                    main.setRules(new File(g.getOptarg()));
                    break;
                case 3:
                    String level = g.getOptarg();
                    if ("jar".equals(level)) {
                        main.setLevel(DepHandler.LEVEL_JAR);
                    } else if ("class".equals(level)) {
                        main.setLevel(DepHandler.LEVEL_CLASS);
                    } else {
                        throw new IllegalArgumentException("unknown level " + level);
                    }
                    break;
                case 4:
                    String style = g.getOptarg();
                    if ("simple".equals(style)) {
                        main.setStyle(STYLE_SIMPLE);
                    } else {
                        throw new IllegalArgumentException("unknown style " + style);
                    }
                    break;
                case 's':
                    strings = true;
                    break;
                case 'f':
                    find = true;
                    break;
                case 'h':
                    help();
                    return;
                case 'v':
                    main.setVerbose(true);
                    break;
                }
            }
            if (find && strings)
                throw new IllegalArgumentException("find and strings cannot be used together");
            int index = g.getOptind();
            int argCount = args.length - index;
            if (argCount == 2) {
                if (find) {
                    main.find(args[index], args[index + 1]);
                } else {
                    main.run(args[index], args[index + 1]);
                }
            } else if (find) {
                main.find(args[index]);
            } else if (strings) {
                main.strings(args[index]);
            } else {
                System.err.println("jarjar: expected two arguments");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("jarjar: " + e.getMessage());
            return;
        }
    }

    private static void help() {
        System.err.print(HELP);
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setRules(File file) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("rules cannot be null");
        patterns = RulesFileParser.parse(file);
    }

    public void setRules(List rules) {
        patterns = new ArrayList(rules);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void find(String arg) throws IOException {
        find(arg, arg);
    }
    
    public void find(String from, String to) throws IOException {
        if (from == null || to == null)
            throw new IllegalArgumentException("arguments cannot be null");
        if (patterns != null)
            throw new IllegalArgumentException("rules cannot be used with find");
        PrintWriter w = new PrintWriter(System.out);
        DepHandler handler = new TextDepHandler(w, level);
        new DepFind().run(from, to, handler);
        w.flush();
    }

    public void strings(String arg) throws IOException {
        if (arg == null)
            throw new IllegalArgumentException("arguments cannot be null");
        if (patterns != null)
            throw new IllegalArgumentException("rules cannot be used with strings");
        new StringDumper().run(arg, new PrintWriter(System.out));
    }

    public void run(String from, String to) throws IOException {
        if (from == null || to == null)
            throw new IllegalArgumentException("arguments cannot be null");
        if (patterns == null)
            throw new IllegalArgumentException("rules are required");
        JarProcessor proc = new MainProcessor(patterns, verbose);
        StandaloneJarProcessor.run(new File(from), new File(to), proc);
    }
}

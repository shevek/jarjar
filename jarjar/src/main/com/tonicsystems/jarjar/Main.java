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
    // TODO: standalone kill?
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            help();
            return;
        }

        Getopt g = new Getopt("jarjar", args, ":fhv", new LongOpt[]{
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'), 
            new LongOpt("rules", LongOpt.REQUIRED_ARGUMENT, null, 2),
            new LongOpt("find", LongOpt.NO_ARGUMENT, null, 'f'),
            new LongOpt("level", LongOpt.REQUIRED_ARGUMENT, null, 3),
            new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, 4),
        });

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
                    } else if ("dot".equals(style)) {
                        main.setStyle(STYLE_DOT);
                    } else {
                        throw new IllegalArgumentException("unknown style " + style);
                    }
                    break;
                case 'f':
                    main.setFind(true);
                    break;
                case 'h':
                    help();
                    return;
                case 'v':
                    main.setVerbose(true);
                    break;
                }
            }
            int index = g.getOptind();
            int argCount = args.length - index;
            if (argCount == 2) {
                main.run(args[index], args[index + 1]);
            } else if (argCount == 1 && main.find) {
                main.run(args[index], args[index]);
            } else {
                System.err.println("jarjar: expected two arguments");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("jarjar: " + e.getMessage());
            return;
        }
    }

    private static void help() {
        // TODO
        System.err.println("I see you asked for help"); // TODO
    }

    public static final int STYLE_SIMPLE = 0;
    public static final int STYLE_DOT = 1;

    private boolean find;
    private boolean verbose;
    private List patterns;
    private int level = DepHandler.LEVEL_CLASS;
    private int style = STYLE_SIMPLE;

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

    public void setFind(boolean find) {
        this.find = find;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void run(String from, String to) throws IOException {
        if (from == null || to == null)
            throw new IllegalArgumentException("arguments cannot be null");
        if ((patterns == null) ^ find)
            throw new IllegalArgumentException("find and rules cannot be used together");

        if (find) {
            PrintWriter w = new PrintWriter(System.out);
            DepHandler handler;
            switch (style) {
            case STYLE_DOT:
                handler = new DotDepHandler(w, level);
                break;
            default:
                handler = new TextDepHandler(w, level);
            }
            new DepFind().run(from, to, handler);
            w.flush();
        } else {
            JarProcessor proc = new MainProcessor(patterns, verbose);
            StandaloneJarProcessor.run(new File(from), new File(to), proc);
        }
    }
}

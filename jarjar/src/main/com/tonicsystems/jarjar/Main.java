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
        });

        try {
            Main main = new Main();
            int c;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                case 2:
                    main.setRules(new File(g.getOptarg()));
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
            if (args.length - index != 2) {
                System.err.println("jarjar: expected two arguments");
            } else {
                main.run(args[index], args[index + 1]);
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

    private boolean find;
    private boolean verbose;
    private List ruleList;
    private List zapList;

    public void setRules(File file) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("rules cannot be null");
        RulesFileParser parser = new RulesFileParser();
        parser.parse(file);
        ruleList = parser.getRuleList();
        zapList = parser.getZapList();
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
        if ((ruleList == null) ^ find)
            throw new IllegalArgumentException("find and rules cannot be used together");

        if (find) {
            Writer w = new PrintWriter(System.out);
            new DepFind().run(from, to, new TextDepHandler(w));
            w.flush();
        } else {
            JarProcessor proc = new MainProcessor(ruleList, zapList, verbose);
            StandaloneJarProcessor.run(new File(from), new File(to), proc);
        }
    }
}

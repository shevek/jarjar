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

import java.util.*;
import java.io.IOException;

class MainProcessor extends JarTransformer
{
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    private RulesImpl rules;
    private List zapList;
    private boolean verbose;
    
    public MainProcessor(List ruleList, List zapList, boolean verbose) {
        rules = new RulesImpl(ruleList, verbose);
        t = new PackageTransformer(rules);
        this.zapList = zapList;
        this.verbose = verbose;
    }
    
    private static String nameFromPath(String path) {
        String name = path.replace('/', '.');
        return name.substring(0, name.length() - 6);
    }

    public boolean process(EntryStruct struct) throws IOException {
        if (struct.file != null && struct.name.equalsIgnoreCase(MANIFEST_PATH)) {
            // TODO: merge manifests?
            if (verbose)
                System.err.println("Ignored " + struct.name);
            return false;
        }
        String oldPath = struct.name;
        if (struct.name.endsWith(".class")) {
            String oldClassName = nameFromPath(struct.name);
            if (zap("L" + struct.name.substring(0, struct.name.length() - 6) + ";")) {
                if (verbose)
                    System.err.println("Zapping " + oldClassName);
                return false;
            }
            super.process(struct);
            // TODO: ensure that we don't end up with duplicate names?
            String newClassName = nameFromPath(struct.name);
            if (verbose && !oldClassName.equals(newClassName))
                System.err.println("Renamed " + oldClassName + " -> " + newClassName);
        } else {
            struct.name = rules.fixPath(struct.name);
            if (verbose) {
                if (struct.name.equals(oldPath)) {
                    System.err.println("Skipped " + struct.name);
                } else {
                    System.err.println("Renamed " + oldPath + " -> " + struct.name);
                }
            }
        }
        return true;
    }

    private boolean zap(String desc) {
        // TODO: optimize
        for (Iterator it = zapList.iterator(); it.hasNext();) {
            if (((Wildcard)it.next()).matches(desc, Wildcard.STYLE_DESC)) {
                return true;
            }
        }
        return false;
    }
}

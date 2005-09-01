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

import com.tonicsystems.jarjar.util.*;
import java.util.*;
import java.io.IOException;

class MainProcessor implements JarProcessor
{
    private boolean verbose;
    private JarProcessor chain;
    
    public MainProcessor(List patterns, boolean verbose) {
        this.verbose = verbose;
        List zapList = new ArrayList();
        List killList = new ArrayList();
        List ruleList = new ArrayList();
        for (Iterator it = patterns.iterator(); it.hasNext();) {
            PatternElement pattern = (PatternElement)it.next();
            if (pattern instanceof Zap) {
                zapList.add(pattern);
            } else if (pattern instanceof Rule) {
                ruleList.add(pattern);
            } else if (pattern instanceof DepKill) {
                killList.add(pattern);
            }
        }
        Rules rules = new RulesImpl(ruleList, verbose);
        chain = new JarProcessorChain(new JarProcessor[]{
            ManifestProcessor.getInstance(),
            new ZapProcessor(zapList),
            new JarTransformerChain(new ClassTransformer[]{
                new DepKillTransformer(killList),
                new PackageTransformer(rules),
            }),
            new ResourceProcessor(rules),
        });
    }

    public boolean process(EntryStruct struct) throws IOException {
        String name = struct.name;
        boolean result = chain.process(struct);
        if (verbose) {
            if (result) {
                if (name.equals(struct.name)) {
                    System.err.println("Renamed " + name + " -> " + struct.name);
                }
            } else {
                System.err.println("Removed " + name);
            }
        }
        return result;
    }
}

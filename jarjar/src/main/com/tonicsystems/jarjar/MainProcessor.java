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
import java.io.File;
import java.io.IOException;
import java.util.*;

class MainProcessor implements JarProcessor
{
    private final boolean verbose;
    private final JarProcessor chain;
    private final KeepProcessor kp;
    private final Map renames = new HashMap();
    
    public MainProcessor(List patterns, boolean verbose, boolean skipManifest) {
        this.verbose = verbose;
        List zapList = new ArrayList();
        List killList = new ArrayList();
        List ruleList = new ArrayList();
        List keepList = new ArrayList();
        for (Iterator it = patterns.iterator(); it.hasNext();) {
            PatternElement pattern = (PatternElement)it.next();
            if (pattern instanceof Zap) {
                zapList.add(pattern);
            } else if (pattern instanceof Rule) {
                ruleList.add(pattern);
            } else if (pattern instanceof Kill) {
                killList.add(pattern);
            } else if (pattern instanceof Keep) {
                keepList.add(pattern);
            }
        }
        if (!killList.isEmpty())
            System.err.println("Kill rules are no longer supported and will be ignored");

        PackageRemapper pr = new PackageRemapper(ruleList, verbose);
        kp = keepList.isEmpty() ? null : new KeepProcessor(keepList);

        List processors = new ArrayList();
        if (skipManifest)
            processors.add(ManifestProcessor.getInstance());
        if (kp != null)
            processors.add(kp);
        // processors.add(new JarClassVisitor(new RemappingClassAdapter(new EmptyVisitor(), kr)));
        processors.add(new ZapProcessor(zapList));
        processors.add(new JarTransformerChain(new ClassTransformer[]{ new RemappingClassTransformer(pr) }));
        processors.add(new ResourceProcessor(pr));
        chain = new JarProcessorChain((JarProcessor[])processors.toArray(new JarProcessor[processors.size()]));
    }

    public void strip(File file) throws IOException {
        if (kp == null)
            return;
        Set excludes = getExcludes();
        if (!excludes.isEmpty())
            StandaloneJarProcessor.run(file, file, new ExcludeProcessor(excludes, verbose));
    }
    
    private Set getExcludes() {
        Set excludes = kp.getExcludes();
        Set result = new HashSet();
        for (Iterator it = excludes.iterator(); it.hasNext();) {
            String name = it.next() + ".class";
            String renamed = (String)renames.get(name);
            result.add((renamed != null) ? renamed : name);
        }
        return result;
    }

    public boolean process(EntryStruct struct) throws IOException {
        String name = struct.name;
        boolean result = chain.process(struct);
        if (result) {
            if (!name.equals(struct.name)) {
                if (kp != null)
                    renames.put(name, struct.name);
                if (verbose)
                    System.err.println("Renamed " + name + " -> " + struct.name);
            }
        } else {
            if (verbose)
                System.err.println("Removed " + name);
        }
        return result;
    }
}

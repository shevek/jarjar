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
        List ruleList = new ArrayList();
        List keepList = new ArrayList();
        for (Iterator it = patterns.iterator(); it.hasNext();) {
            PatternElement pattern = (PatternElement)it.next();
            if (pattern instanceof Zap) {
                zapList.add(pattern);
            } else if (pattern instanceof Rule) {
                ruleList.add(pattern);
            } else if (pattern instanceof Keep) {
                keepList.add(pattern);
            }
        }

        PackageRemapper pr = new PackageRemapper(ruleList, verbose);
        kp = keepList.isEmpty() ? null : new KeepProcessor(keepList);

        List processors = new ArrayList();
        if (skipManifest)
            processors.add(ManifestProcessor.getInstance());
        if (kp != null)
            processors.add(kp);
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

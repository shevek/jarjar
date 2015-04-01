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

import com.tonicsystems.jarjar.transform.asm.PackageRemapper;
import com.tonicsystems.jarjar.transform.jar.PathFilterJarProcessor;
import com.tonicsystems.jarjar.transform.jar.ManifestFilterJarProcessor;
import com.tonicsystems.jarjar.transform.jar.ResourceRenamerJarProcessor;
import com.tonicsystems.jarjar.transform.EntryStruct;
import com.tonicsystems.jarjar.transform.StandaloneJarProcessor;
import com.tonicsystems.jarjar.transform.jar.ClassClosureFilterJarProcessor;
import com.tonicsystems.jarjar.transform.jar.JarProcessor;
import com.tonicsystems.jarjar.transform.jar.JarProcessorChain;
import com.tonicsystems.jarjar.transform.jar.ClassFilterJarProcessor;
import com.tonicsystems.jarjar.transform.config.PatternElement;
import com.tonicsystems.jarjar.transform.config.Zap;
import com.tonicsystems.jarjar.transform.config.Keep;
import com.tonicsystems.jarjar.transform.config.Rule;
import com.tonicsystems.jarjar.transform.asm.RemappingClassTransformer;
import com.tonicsystems.jarjar.transform.jar.ClassTransformerJarProcessor;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MainProcessor.class);
    private final JarProcessorChain chain;
    private final ClassClosureFilterJarProcessor kp;
    private final Map<String, String> renames = new HashMap<String, String>();

    public MainProcessor(@Nonnull List<PatternElement> patterns, boolean skipManifest) {
        List<Zap> zapList = new ArrayList<Zap>();
        List<Rule> ruleList = new ArrayList<Rule>();
        List<Keep> keepList = new ArrayList<Keep>();
        for (PatternElement pattern : patterns) {
            if (pattern instanceof Zap) {
                zapList.add((Zap) pattern);
            } else if (pattern instanceof Rule) {
                ruleList.add((Rule) pattern);
            } else if (pattern instanceof Keep) {
                keepList.add((Keep) pattern);
            }
        }

        PackageRemapper pr = new PackageRemapper(ruleList);
        kp = keepList.isEmpty() ? null : new ClassClosureFilterJarProcessor(keepList);

        List<JarProcessor> processors = new ArrayList<JarProcessor>();
        if (skipManifest)
            processors.add(ManifestFilterJarProcessor.getInstance());
        if (kp != null)
            processors.add(kp);
        processors.add(new ClassFilterJarProcessor(zapList));
        processors.add(new ClassTransformerJarProcessor(new RemappingClassTransformer(pr)));
        processors.add(new ResourceRenamerJarProcessor(pr));
        chain = new JarProcessorChain(processors.toArray(new JarProcessor[processors.size()]));
    }

    public void strip(File file) throws IOException {
        if (kp == null)
            return;
        Set<String> excludes = getExcludes();
        if (!excludes.isEmpty())
            StandaloneJarProcessor.run(file, file, new PathFilterJarProcessor(excludes));
    }

    /**
     * Returns the <code>.class</code> files to delete. As well the root-parameter as the rename ones
     * are taken in consideration, so that the concerned files are not listed in the result.
     *
     * @return the paths of the files in the jar-archive, including the <code>.class</code> suffix
     */
    private Set<String> getExcludes() {
        Set<String> result = new HashSet<String>();
        for (String exclude : kp.getExcludes()) {
            String name = exclude + ".class";
            String renamed = renames.get(name);
            result.add((renamed != null) ? renamed : name);
        }
        return result;
    }

    /**
     *
     * @param struct
     * @return <code>true</code> if the entry is to include in the output jar
     * @throws IOException
     */
    @Override
    public boolean process(EntryStruct struct) throws IOException {
        String name = struct.name;
        boolean keepIt = chain.process(struct);
        if (keepIt) {
            if (!name.equals(struct.name)) {
                if (kp != null)
                    renames.put(name, struct.name);
                if (LOG.isDebugEnabled())
                    LOG.debug("Renamed " + name + " -> " + struct.name);
            }
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Removed " + name);
        }
        return keepIt;
    }
}

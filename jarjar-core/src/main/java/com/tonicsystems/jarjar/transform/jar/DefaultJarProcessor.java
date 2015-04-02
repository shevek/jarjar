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
package com.tonicsystems.jarjar.transform.jar;

import com.tonicsystems.jarjar.transform.asm.PackageRemapper;
import com.tonicsystems.jarjar.transform.config.Zap;
import com.tonicsystems.jarjar.transform.config.Keep;
import com.tonicsystems.jarjar.transform.config.Rule;
import com.tonicsystems.jarjar.transform.asm.RemappingClassTransformer;
import com.tonicsystems.jarjar.transform.config.RulesFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJarProcessor extends JarProcessorChain implements RulesFileParser.Output {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJarProcessor.class);
    // private final Map<String, String> renames = new HashMap<String, String>();

    private final ManifestFilterJarProcessor manifestFilterJarProcessor = new ManifestFilterJarProcessor();
    private final ClassFilterJarProcessor classFilterJarProcessor = new ClassFilterJarProcessor();
    private final ClassClosureFilterJarProcessor classClosureFilterJarProcessor = new ClassClosureFilterJarProcessor();
    private final PackageRemapper packageRemapper = new PackageRemapper();
    private final RemappingClassTransformer remappingClassTransformer = new RemappingClassTransformer(packageRemapper);
    private final ResourceRenamerJarProcessor resourceRenamerJarProcessor = new ResourceRenamerJarProcessor(packageRemapper);

    public DefaultJarProcessor() {
        add(manifestFilterJarProcessor);
        add(classFilterJarProcessor);
        add(classClosureFilterJarProcessor);
        add(new ClassTransformerJarProcessor(remappingClassTransformer));
        add(resourceRenamerJarProcessor);
    }

    @Override
    public void addZap(Zap zap) {
        classFilterJarProcessor.addZap(zap);
    }

    @Override
    public void addKeep(Keep keep) {
        classClosureFilterJarProcessor.addKeep(keep);
    }

    @Override
    public void addRule(Rule rule) {
        packageRemapper.addRule(rule);
    }

    public void setSkipManifest(boolean value) {
        manifestFilterJarProcessor.setEnabled(value);
    }
}

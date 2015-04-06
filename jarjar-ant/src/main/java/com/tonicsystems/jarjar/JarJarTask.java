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

import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor;
import com.tonicsystems.jarjar.transform.config.ClassRename;
import com.tonicsystems.jarjar.transform.config.ClassDelete;
import com.tonicsystems.jarjar.transform.config.ClassClosureRoot;
import com.tonicsystems.jarjar.util.AntJarProcessor;
import org.apache.tools.ant.BuildException;

public class JarJarTask extends AntJarProcessor {

    private DefaultJarProcessor processor = new DefaultJarProcessor();

    public void addConfiguredRule(ClassRename rule) {
        if (rule.getPattern() == null || rule.getResult() == null)
            throw new IllegalArgumentException("The <rule> element requires both \"pattern\" and \"result\" attributes.");
        processor.addRule(rule);
    }

    public void addConfiguredZap(ClassDelete zap) {
        if (zap.getPattern() == null)
            throw new IllegalArgumentException("The <zap> element requires a \"pattern\" attribute.");
        processor.addZap(zap);
    }

    public void addConfiguredKeep(ClassClosureRoot keep) {
        if (keep.getPattern() == null)
            throw new IllegalArgumentException("The <keep> element requires a \"pattern\" attribute.");
        processor.addKeep(keep);
    }

    @Override
    public void execute() throws BuildException {
        execute(processor);
    }

    @Override
    protected void cleanHelper() {
        super.cleanHelper();
        processor = new DefaultJarProcessor();
    }
}

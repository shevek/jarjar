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

import com.tonicsystems.jarjar.Wildcard;
import com.tonicsystems.jarjar.config.Keep;
import com.tonicsystems.jarjar.util.EntryStruct;
import com.tonicsystems.jarjar.transform.jar.JarProcessor;
import com.tonicsystems.jarjar.util.ClassNameUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: this can probably be refactored into JarClassVisitor, etc.
public class KeepProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KeepProcessor.class);

    private static class DependencyCollector extends Remapper {

        private final Set<String> dependencies = new HashSet<String>();

        @Override
        public String map(String key) {
            if (key.startsWith("java/") || key.startsWith("javax/"))
                return null;
            dependencies.add(key);
            return null;
        }

        @Override
        public Object mapValue(Object value) {
            if (value instanceof String) {
                String s = (String) value;
                if (ClassNameUtils.isArrayForName(s)) {
                    mapDesc(s.replace('.', '/'));
                } else if (ClassNameUtils.isForName(s)) {
                    map(s.replace('.', '/'));
                }
                return value;
            } else {
                return super.mapValue(value);
            }
        }
    }

    private final List<Wildcard> wildcards;
    private final List<String> roots = new ArrayList<String>();
    private final Map<String, Set<String>> dependencies = new HashMap<String, Set<String>>();

    public KeepProcessor(@Nonnull List<Keep> patterns) {
        wildcards = Wildcard.createWildcards(patterns);
    }

    public boolean isEnabled() {
        return !wildcards.isEmpty();
    }

    /**
     * Returns the list of namees to remove from the generated JAR file.
     *
     * Computes the transitive set of reachable names from the root set.
     * Removes that from the overall set of names seen in the JAR file.
     *
     * @return the residue.
     */
    @Nonnull
    public Set<String> getExcludes() {
        Set<String> keep = new HashSet<String>();
        closureHelper(keep, roots);
        Set<String> remove = new HashSet<String>(dependencies.keySet());
        remove.removeAll(keep);
        return remove;
    }

    private void closureHelper(Set<String> closure, Collection<String> process) {
        if (process == null)
            return;
        for (String name : process) {
            if (closure.add(name))
                closureHelper(closure, dependencies.get(name));
        }
    }

    @Override
    public boolean process(EntryStruct struct) throws IOException {
        try {
            if (struct.name.endsWith(".class")) {
                String name = struct.name.substring(0, struct.name.length() - 6);
                for (Wildcard wildcard : wildcards)
                    if (wildcard.matches(name))
                        roots.add(name);
                DependencyCollector collector = new DependencyCollector();
                dependencies.put(name, collector.dependencies);
                new ClassReader(new ByteArrayInputStream(struct.data)).accept(new RemappingClassAdapter(null, collector), ClassReader.EXPAND_FRAMES);
                collector.dependencies.remove(name);
            }
        } catch (Exception e) {
            LOG.warn("Error reading " + struct.name + ": " + e.getMessage());
        }
        return true;
    }
}
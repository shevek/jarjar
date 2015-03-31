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

import com.tonicsystems.jarjar.transform.asm.ClassTransform;
import com.tonicsystems.jarjar.transform.asm.GetNameClassWriter;
import com.tonicsystems.jarjar.transform.jar.JarProcessor;
import com.tonicsystems.jarjar.util.EntryStruct;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassTransformProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTransformProcessor.class);
    private final List<ClassTransform> classProcessors;

    public ClassTransformProcessor(@Nonnull List<ClassTransform> classProcessors) {
        this.classProcessors = classProcessors;
    }

    public ClassTransformProcessor(@Nonnull ClassTransform classProcessors) {
        this(Arrays.asList(classProcessors));
    }

    @Override
    public boolean process(EntryStruct struct) throws IOException {
        if (struct.name.endsWith(".class")) {
            try {
                ClassReader reader = new ClassReader(struct.data);
                GetNameClassWriter w = new GetNameClassWriter(ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = w;
                for (ClassTransform classProcessor : classProcessors)
                    cv = classProcessor.transform(cv);
                reader.accept(cv, ClassReader.EXPAND_FRAMES);
                struct.data = w.toByteArray();
                struct.name = pathFromName(w.getClassName());
            } catch (Exception e) {
                LOG.warn("Failed to read class " + struct.name + ": " + e);
            }
        }
        return true;
    }

    private static String pathFromName(String className) {
        return className.replace('.', '/') + ".class";
    }
}

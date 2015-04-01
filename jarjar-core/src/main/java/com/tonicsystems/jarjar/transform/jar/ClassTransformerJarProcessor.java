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

import com.tonicsystems.jarjar.transform.asm.ClassTransformer;
import com.tonicsystems.jarjar.transform.asm.GetNameClassWriter;
import com.tonicsystems.jarjar.transform.EntryStruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JarProcessor which applies a list of {@link ClassTransformer ClassTransformers}
 * to any files ending in .class.
 */
public class ClassTransformerJarProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTransformerJarProcessor.class);
    private final List<ClassTransformer> classProcessors;

    public ClassTransformerJarProcessor(@Nonnull List<ClassTransformer> classProcessors) {
        this.classProcessors = classProcessors;
    }

    public ClassTransformerJarProcessor(@Nonnull ClassTransformer classProcessors) {
        this(Arrays.asList(classProcessors));
    }

    @Override
    public Result scan(EntryStruct struct) throws IOException {
        return Result.KEEP;
    }

    @Override
    public Result process(EntryStruct struct) throws IOException {
        if (struct.name.endsWith(".class")) {
            try {
                ClassReader reader = new ClassReader(struct.data);
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                GetNameClassWriter namer = new GetNameClassWriter(writer);
                ClassVisitor cv = namer;
                for (ClassTransformer classProcessor : classProcessors)
                    cv = classProcessor.transform(cv);
                reader.accept(cv, ClassReader.EXPAND_FRAMES);
                struct.name = pathFromName(namer.getClassName());
                struct.data = writer.toByteArray();
            } catch (Exception e) {
                LOG.warn("Failed to read class " + struct.name + ": " + e);
            }
        }
        return Result.KEEP;
    }

    private static String pathFromName(String className) {
        return className.replace('.', '/') + ".class";
    }
}

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
package com.tonicsystems.jarjar.transform.asm;

import javax.annotation.Nonnull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class GetNameClassWriter extends ClassVisitor {

    private String className;

    /**
     * Constructs a new GetNameClassWriter.
     */
     // * @param flags may include {@link ClassWriter#COMPUTE_FRAMES} * or {@link ClassWriter#COMPUTE_MAXS}.
    public GetNameClassWriter(ClassVisitor cv) {
        super(Opcodes.ASM8, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Nonnull
    public String getClassName() {
        return className;
    }
}

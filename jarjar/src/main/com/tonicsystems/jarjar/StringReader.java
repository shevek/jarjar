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
import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;

abstract class StringReader extends EmptyVisitor
{
    private int line = -1;
    private String className;

    abstract public void visitString(String className, String value, int line);

    private void handleObject(Object value) {
        if (value instanceof String)
            visitString(className, (String)value, line);
    }

    public void visit(String name, Object value) {
        handleObject(value);
    }
    
    public void visitEnum(String name, String desc, String value) {
        handleObject(value);
    }
    
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        line = -1;
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        handleObject(value);
        return this;
    }

    public void visitLdcInsn(Object cst) {
        handleObject(cst);
    }
    
    public void visitLineNumber(int line, Label start) {
        this.line = line;
    }
}

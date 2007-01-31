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
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;

class PackageTransformer extends ClassAdapter implements ClassTransformer
{
    private Rules rules;
    private String className;
    
    public PackageTransformer(Rules rules) {
        super(null);
        this.rules = rules;
    }
    
    public void setTarget(ClassVisitor target) {
        cv = target;
    }

    private String[] fixNames(String[] names) {
        if (names == null)
            return null;
        String[] fixed = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            fixed[i] = rules.fixName(names[i]);
        }
        return fixed;
    }

    private Object fixValue(Object value) {
        if (value instanceof String) {
            return rules.fixString(className, (String)value);
        } else if (value instanceof Type) {
            return Type.getType(rules.fixDesc(((Type)value).getDescriptor()));
        } else {
            return value;
        }
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name.replace('/', '.');
        cv.visit(version, access, rules.fixName(name), fixSignature(signature, false), rules.fixName(superName), fixNames(interfaces));
    }

    public void visitAttribute(Attribute attr) {
        cv.visitAttribute(rules.fixAttribute(attr));
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new AnnotationFixer(cv.visitAnnotation(rules.fixDesc(desc), visible));
    }
    
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = cv.visitField(access, name, rules.fixDesc(desc), fixSignature(signature, true), fixValue(value));
        return (fv != null) ? new FieldFixer(fv) : null;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cv.visitInnerClass(rules.fixName(name), rules.fixName(outerName), innerName, access);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        cv.visitOuterClass(rules.fixName(owner), name, (desc != null) ? rules.fixMethodDesc(desc) : null);
    }
    
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, rules.fixMethodDesc(desc), fixSignature(signature, false), fixNames(exceptions));
        return (mv != null) ? new MethodFixer(mv) : null;
    }

    private class FieldFixer implements FieldVisitor
    {
        private FieldVisitor fv;

        public FieldFixer(FieldVisitor fv) {
            this.fv = fv;
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationFixer(fv.visitAnnotation(rules.fixDesc(desc), visible));
        }
        
        public void visitAttribute(Attribute attr) {
            fv.visitAttribute(rules.fixAttribute(attr));
        }
        
        public void visitEnd() {
            fv.visitEnd();
        }
    }

    private class MethodFixer extends MethodAdapter
    {
        public MethodFixer(MethodVisitor mv) {
            super(mv);
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return fixAnnotation(mv.visitAnnotation(rules.fixDesc(desc), visible));
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return fixAnnotation(mv.visitAnnotationDefault());
        }

        public void visitTypeInsn(int opcode, String desc) {
            mv.visitTypeInsn(opcode, (desc.charAt(0) == '[') ? rules.fixDesc(desc) : rules.fixName(desc));
        }

        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            mv.visitFieldInsn(opcode, rules.fixName(owner), name, rules.fixDesc(desc));
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            mv.visitMethodInsn(opcode, rules.fixName(owner), name, rules.fixMethodDesc(desc));
        }

        public void visitLdcInsn(Object cst) {
            mv.visitLdcInsn(fixValue(cst));
        }

        public void visitMultiANewArrayInsn(String desc, int dims) {
            mv.visitMultiANewArrayInsn(rules.fixDesc(desc), dims);
        }

        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            mv.visitTryCatchBlock(start, end, handler, rules.fixName(type));
        }

        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            mv.visitLocalVariable(name, rules.fixDesc(desc), fixSignature(signature, true), start, end, index);
        }

        public void visitAttribute(Attribute attr) {
            mv.visitAttribute(rules.fixAttribute(attr));
        }
    }

    private class AnnotationFixer implements AnnotationVisitor
    {
        private AnnotationVisitor av;
        
        public AnnotationFixer(AnnotationVisitor av) {
            this.av = av;
        }
        
        public void visit(String name, Object value) {
            av.visit(name, fixValue(value));
        }

        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return fixAnnotation(av.visitAnnotation(name, rules.fixDesc(desc)));
        }

        public AnnotationVisitor visitArray(String name) {
            return fixAnnotation(av.visitArray(name));
        }

        public void visitEnum(String name, String desc, String value) {
            av.visitEnum(name, rules.fixDesc(desc), (String)fixValue(value));
        }

        public void visitEnd() {
            av.visitEnd();
        }
    }

    private AnnotationVisitor fixAnnotation(AnnotationVisitor av) {
        return (av != null) ? new AnnotationFixer(av) : null;
    }

    private String fixSignature(String signature, boolean type) {
        if (signature == null)
            return null;
        SignatureReader reader = new SignatureReader(signature);
        SignatureWriter writer = new SignatureWriter();
        SignatureFixer fixer = new SignatureFixer(writer);
        if (type) {
            reader.acceptType(fixer);
        } else {
            reader.accept(fixer);
        }
        return writer.toString();
    }
    
    private class SignatureFixer extends SignatureAdapter
    {
        public SignatureFixer(SignatureWriter sw) {
            super(sw);
        }

        public void visitTypeVariable(String name) {
            sw.visitTypeVariable(rules.fixName(name));
        }

        public void visitClassType(String name) {
            sw.visitClassType(rules.fixName(name));
        }
    
        public void visitInnerClassType(String name) {
            sw.visitInnerClassType(rules.fixName(name));
        }
    }
}

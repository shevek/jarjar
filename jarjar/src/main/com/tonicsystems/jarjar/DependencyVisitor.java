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

abstract class DependencyVisitor extends ClassAdapter
{
    private String className;
    
    public DependencyVisitor(ClassVisitor cv) {
        super(cv);
    }
    
    abstract protected String fixDesc(String desc);
    abstract protected String fixName(String name);
    abstract protected String fixMethodDesc(String desc);
    abstract protected String fixString(String className, String value);
    abstract protected Attribute fixAttribute(Attribute attrs);

    private String[] fixNames(String[] names) {
        if (names == null)
            return null;
        String[] fixed = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            fixed[i] = fixName(names[i]);
        }
        return fixed;
    }

    private Object fixValue(Object value) {
        if (value instanceof String) {
            return fixString(className, (String)value);
        } else if (value instanceof Type) {
            return Type.getType(fixDesc(((Type)value).getDescriptor()));
        } else {
            return value;
        }
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name.replace('/', '.');
        cv.visit(version, access, fixName(name), fixSignature(signature, false), fixName(superName), fixNames(interfaces));
    }

    public void visitAttribute(Attribute attr) {
        cv.visitAttribute(fixAttribute(attr));
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new AnnotationFixer(cv.visitAnnotation(fixDesc(desc), visible));
    }
    
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = cv.visitField(access, name, fixDesc(desc), fixSignature(signature, true), fixValue(value));
        return (fv != null) ? new FieldFixer(fv) : null;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cv.visitInnerClass(fixName(name), fixName(outerName), innerName, access);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        cv.visitOuterClass(fixName(owner), name, (desc != null) ? fixMethodDesc(desc) : null);
    }
    
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, fixMethodDesc(desc), fixSignature(signature, false), fixNames(exceptions));
        return (mv != null) ? new MethodFixer(mv) : null;
    }

    public void visitEnd() {
        cv.visitEnd();
    }

    public void visitSource(String source, String debug) {
        cv.visitSource(source, debug);
    }

    private class FieldFixer implements FieldVisitor
    {
        private FieldVisitor fv;

        public FieldFixer(FieldVisitor fv) {
            this.fv = fv;
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationFixer(fv.visitAnnotation(fixDesc(desc), visible));
        }
        
        public void visitAttribute(Attribute attr) {
            fv.visitAttribute(fixAttribute(attr));
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
            return fixAnnotation(mv.visitAnnotation(fixDesc(desc), visible));
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return fixAnnotation(mv.visitAnnotationDefault());
        }

        public void visitTypeInsn(int opcode, String desc) {
            mv.visitTypeInsn(opcode, (desc.charAt(0) == '[') ? fixDesc(desc) : fixName(desc));
        }

        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            mv.visitFieldInsn(opcode, fixName(owner), name, fixDesc(desc));
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            mv.visitMethodInsn(opcode, fixName(owner), name, fixMethodDesc(desc));
        }

        public void visitLdcInsn(Object cst) {
            mv.visitLdcInsn(fixValue(cst));
        }

        public void visitMultiANewArrayInsn(String desc, int dims) {
            mv.visitMultiANewArrayInsn(fixDesc(desc), dims);
        }

        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            mv.visitTryCatchBlock(start, end, handler, fixName(type));
        }

        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            mv.visitLocalVariable(name, fixDesc(desc), fixSignature(signature, true), start, end, index);
        }

        public void visitAttribute(Attribute attr) {
            mv.visitAttribute(fixAttribute(attr));
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
            return fixAnnotation(av.visitAnnotation(name, fixDesc(desc)));
        }

        public AnnotationVisitor visitArray(String name) {
            return fixAnnotation(av.visitArray(name));
        }

        public void visitEnum(String name, String desc, String value) {
            av.visitEnum(name, fixDesc(desc), (String)fixValue(value));
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
            sw.visitTypeVariable(fixName(name));
        }

        public void visitClassType(String name) {
            sw.visitClassType(fixName(name));
        }
    
        public void visitInnerClassType(String name) {
            sw.visitInnerClassType(fixName(name));
        }
    }
}

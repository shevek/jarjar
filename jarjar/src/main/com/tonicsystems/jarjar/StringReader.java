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

import org.objectweb.asm.*;

class StringReader
extends NullClassVisitor
{
    private StringVisitor sv;
    private int line = -1;

    public StringReader(StringVisitor sv) {
        this.sv = sv;
    }

    private AnnotationVisitor av = new AnnotationVisitor() {
        public void visit(String name, Object value) {
            handleObject(value);
        }
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return this;
        }
        public AnnotationVisitor visitArray(String name) {
            return this;
        }
        public void visitEnum(String name, String desc, String value) {
            handleObject(value);
        }
        public void visitEnd() { }
    };

    private void handleObject(Object value) {
        if (value instanceof String)
            sv.visitString((String)value, line);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        line = -1;
        sv.visitStart(name);
    }

    public void visitEnd() {
        sv.visitEnd();
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        handleObject(value);
        return new FieldVisitor() {
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return av;
            }
            public void visitAttribute(Attribute attr) { }
            public void visitEnd() { }
        };
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new NullMethodVisitor() {
            public AnnotationVisitor visitAnnotationDefault() {
                return av;
            }
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return av;
            }
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                return av;
            }
            public void visitLdcInsn(Object cst) {
                handleObject(cst);
            }
            public void visitLineNumber(int line, Label start) {
                StringReader.this.line = line;
            }
        };
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return av;
    }
}
        


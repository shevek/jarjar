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

class NullMethodVisitor implements MethodVisitor
{
    private static final NullMethodVisitor INSTANCE = new NullMethodVisitor();

    public static NullMethodVisitor getInstance() {
        return INSTANCE;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return NullAnnotationVisitor.getInstance();
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return NullAnnotationVisitor.getInstance();
    }
    
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return NullAnnotationVisitor.getInstance();
    }
    
    public void visitAttribute(Attribute attr) { }
    public void visitEnd() { }
    public void visitFieldInsn(int opcode, String owner, String name, String desc) { }
    public void visitIincInsn(int var, int increment) { }
    public void visitInsn(int opcode) { }
    public void visitIntInsn(int opcode, int operand) { }
    public void visitJumpInsn(int opcode, Label label) { }
    public void visitLabel(Label label) { }
    public void visitLdcInsn(Object cst) { }
    public void visitLineNumber(int line, Label start) { }
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) { }
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) { }
    public void visitMaxs(int maxStack, int maxLocals) { }
    public void visitMethodInsn(int opcode, String owner, String name, String desc) { }
    public void visitMultiANewArrayInsn(String desc, int dims) { }
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) { }
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) { }
    public void visitTypeInsn(int opcode, String desc) { }
    public void visitVarInsn(int opcode, int var) { }
}

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

import java.util.*;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class DepKillTransformer extends ClassAdapter implements ClassTransformer
{
    private static final Type TYPE_OBJECT = Type.getType(Object.class);
    private Wildcard[] wildcards;

    public DepKillTransformer(List patterns) {
        super(null);
        wildcards = PatternElement.createWildcards(patterns);
    }

    public void setTarget(ClassVisitor target) {
        cv = target;
    }

    private boolean checkDesc(String desc) {
        for (int i = 0; i < wildcards.length; i++) {
            if (wildcards[i].matches(desc, Wildcard.STYLE_DESC))
                return true;
        }
        return false;
    }

    private String fixMethodDesc(String methodDesc) {
        if (wildcards.length == 0)
            return methodDesc;
        Type[] args = Type.getArgumentTypes(methodDesc);
        for (int i = 0; i < args.length; i++)
            args[i] = eraseType(args[i]);
        return Type.getMethodDescriptor(eraseType(Type.getReturnType(methodDesc)), args);
    }

    private Type eraseType(Type type) {
        return checkDesc(type.getDescriptor()) ? TYPE_OBJECT : type;
    }

    private boolean checkName(String name) {
        if (wildcards.length == 0)
            return false;
        return checkDesc("L" + name + ";");
    }

    private static void replace(MethodVisitor mv, String desc) {
        switch (desc.charAt(0)) {
        case 'V':
            break;
        case 'D':
            mv.visitInsn(Opcodes.DCONST_0);
            break;
        case 'F':
            mv.visitInsn(Opcodes.FCONST_0);
            break;
        case 'J':
            mv.visitInsn(Opcodes.LCONST_0);
            break;
        case 'C':
        case 'S':
        case 'B':
        case 'I':
        case 'Z':
            mv.visitInsn(Opcodes.ICONST_0);
            break;
        case 'L':
        case '[':
            mv.visitInsn(Opcodes.ACONST_NULL);
            break;
        }
    }

    private static void pop(MethodVisitor mv, String desc) {
        switch (desc.charAt(0)) {
        case 'D':
        case 'J':
            mv.visitInsn(Opcodes.POP2);
        default:
            mv.visitInsn(Opcodes.POP);
        }
    }
                
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (exceptions != null && wildcards.length > 0) {
            List exceptionList = new ArrayList(exceptions.length);
            for (int i = 0; i < exceptions.length; i++) {
                if (!checkName(exceptions[i]))
                    exceptionList.add(exceptions[i]);
            }
            exceptions = (String[])exceptionList.toArray(new String[exceptionList.size()]);
        }
        return new DepKillMethodVisitor(cv.visitMethod(access, name, fixMethodDesc(desc), signature, exceptions));
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (checkDesc(desc)) {
            // System.err.println("visitField " + desc);
            desc = TYPE_OBJECT.getDescriptor();
        }
        return super.visitField(access, name, desc, signature, value);
    }

    private class DepKillMethodVisitor extends MethodAdapter
    {
        public DepKillMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        public void visitTypeInsn(int opcode, String desc) {
            if ((desc.charAt(0) == '[') ? checkDesc(desc) : checkName(desc)) {
                // System.err.println("visitTypeInsn " + desc);
                switch (opcode) {
                case Opcodes.NEW:
                case Opcodes.ANEWARRAY:
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    break;
                case Opcodes.CHECKCAST:
                case Opcodes.INSTANCEOF:
                    mv.visitInsn(Opcodes.ICONST_0);
                    break;
                }
            } else {
                mv.visitTypeInsn(opcode, desc);
            }
        }

        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (checkName(owner) || checkDesc(desc)) {
                // System.err.println("visitFieldInsn " + owner + ", " + desc);
                switch (opcode) {
                case Opcodes.GETFIELD:
                    mv.visitInsn(Opcodes.POP);
                    replace(mv, desc);
                    break;
                case Opcodes.PUTFIELD:
                    pop(mv, desc);
                    mv.visitInsn(Opcodes.POP);
                    break;
                case Opcodes.GETSTATIC:
                    replace(mv, desc);
                    break;
                case Opcodes.PUTSTATIC:
                    pop(mv, desc);
                    break;
                }
            } else {
                mv.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (checkName(owner)) {
                // System.err.println("visitMethodInsn " + owner + ", " + desc + " (" + name + ")");
                switch (opcode) {
                case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKEVIRTUAL:
                    mv.visitInsn(Opcodes.POP);
                    break;
                case Opcodes.INVOKESPECIAL:
                    throw new IllegalStateException("Cannot remove invocation of " + owner + "." + desc);
                case Opcodes.INVOKESTATIC:
                }

                Type[] args = Type.getArgumentTypes(desc);
                for (int i = 0; i < args.length; i++)
                    mv.visitInsn((args[i].getSize() == 2) ? Opcodes.POP2 : Opcodes.POP);
                replace(mv, Type.getReturnType(desc).getDescriptor());

            } else {
                mv.visitMethodInsn(opcode, owner, name, fixMethodDesc(desc));
            }
        }

        public void visitMultiANewArrayInsn(String desc, int dims) {
            if (checkDesc(desc)) {
                // System.err.println("visitMultiANewArrayInsn " + desc);
                mv.visitInsn(Opcodes.ACONST_NULL);
            } else {
                mv.visitMultiANewArrayInsn(desc, dims);
            }
        }

        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (!checkName(type))
                mv.visitTryCatchBlock(start, end, handler, type);
        }

        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            if (checkDesc(desc)) {
                // System.err.println("visitLocalVariable " + desc);
                desc = TYPE_OBJECT.getDescriptor();
            }
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        }

        public void visitAttribute(Attribute attr) {
            mv.visitAttribute(attr);
        }
    }
}

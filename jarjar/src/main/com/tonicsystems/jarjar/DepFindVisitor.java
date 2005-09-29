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
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;
import org.objectweb.asm.commons.EmptyVisitor;

// TODO: annotations
// TODO: field visitor
class DepFindVisitor extends EmptyVisitor
{
    private Map classes;
    private String source;
    private String curName;
    private DepHandler handler;
    private PathClass curPathClass;
    
    public DepFindVisitor(Map classes, Object source, DepHandler handler) throws IOException {
        this.classes = classes;
        this.source = getSourceName(source);
        this.handler = handler;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        curName = name;
        curPathClass = new PathClass(source, curName);
        checkSignature(signature, false);
        checkName(superName);
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++)
                checkName(interfaces[i]);
        }
    }

    private void checkSignature(String signature, boolean type) {
        if (signature != null) {
            SignatureReader reader = new SignatureReader(signature);
            SignatureVisitor checker = new SignatureChecker();
            if (type) {
                reader.acceptType(checker);
            } else {
                reader.accept(checker);
            }    
        }
    }

    private class SignatureChecker extends EmptySignatureVisitor
    {
        public void visitTypeVariable(String name) {
            checkName(name);
        }

        public void visitClassType(String name) {
            checkName(name);
        }
    
        public void visitInnerClassType(String name) {
            checkName(name);
        }
    };
    
    private void checkDesc(String desc) {
        int index = desc.indexOf('L');
        if (index >= 0)
            checkName(desc.substring(index + 1, desc.length() - 1));
    }

    private void checkMethodDesc(String methodDesc) {
        checkDesc(Type.getReturnType(methodDesc).getDescriptor());
        Type[] args = Type.getArgumentTypes(methodDesc);
        for (int i = 0; i < args.length; i++)
            checkDesc(args[i].getDescriptor());
    }

    private String getSourceName(Object source) throws IOException {
        if (source instanceof ZipFile) {
            return ((ZipFile)source).getName();
        } else {
            return ((File)source).getCanonicalPath();
        }
    }

    private void checkName(String name) {
        try {
            if (classes.containsKey(name)) {
                String otherSource = getSourceName(classes.get(name));
                if (!source.equals(otherSource)) {
                    // TODO: some escape mechanism?
                    handler.handle(curPathClass, new PathClass(otherSource, name));
                }
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        checkMethodDesc(desc);
        checkSignature(signature, false);
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; i++)
                checkName(exceptions[i]);
        }
        return this;
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        checkDesc(desc);
        checkSignature(signature, true);
        return null; // TODO?
    }

    public void visitTypeInsn(int opcode, String desc) {
        if (desc.charAt(0) == '[') {
            checkDesc(desc);
        } else {
            checkName(desc);
        }
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        checkName(owner);
        checkDesc(desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        checkName(owner);
        checkMethodDesc(desc);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        checkDesc(desc);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        checkName(type);
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        checkDesc(desc);
        checkSignature(signature, true);
    }
}

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

import com.tonicsystems.jarjar.cglib.NullClassVisitor;
import com.tonicsystems.jarjar.cglib.Signature;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;

class DepFindVisitor
extends NullClassVisitor
{
    private Map classes;
    private String source;
    private String curName;
    private CodeVisitor code = new DepFindCodeVisitor();
    
    public DepFindVisitor(Map classes, Object source)
    throws IOException
    {
        this.classes = classes;
        this.source = getSourceName(source);
    }

    public void visit(int access, String name, String superName, String[] interfaces, String sourceFile)
    {
        curName = name;
    }

    private void checkDesc(String desc)
    {
        int index = desc.indexOf('L');
        if (index >= 0)
            checkName(desc.substring(index + 1, desc.length() - 1));
    }

    private void checkMethodDesc(String methodDesc)
    {
        Signature sig = new Signature("foo", methodDesc);
        Type[] args = sig.getArgumentTypes();
        for (int i = 0; i < args.length; i++)
            checkDesc(args[i].getDescriptor());
    }

    private String getSourceName(Object source)
    throws IOException
    {
        if (source instanceof ZipFile) {
            return ((ZipFile)source).getName();
        } else {
            return ((File)source).getCanonicalPath();
        }
    }

    private void checkName(String name)
    {
        try {
            if (classes.containsKey(name) && !source.equals(getSourceName(classes.get(name))))
                throw new DepFindException(curName, name);
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    public CodeVisitor visitMethod(int access, String name, String desc, String[] exceptions, Attribute attrs)
    {
        checkMethodDesc(desc);
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length; i++)
                checkName(exceptions[i]);
        }
        return code;
    }

    public void visitField(int access, String name, String desc, Object value, Attribute attrs)
    {
        checkDesc(desc);
    }

    private class DepFindCodeVisitor
    extends CodeAdapter
    {
        public DepFindCodeVisitor()
        {
            super(NullClassVisitor.INSTANCE.visitMethod(0, null, null, null, null)); // TODO: ugly
        }
        
        public void visitTypeInsn(int opcode, String desc)
        {
            if (desc.charAt(0) == '[') {
                checkDesc(desc);
            } else {
                checkName(desc);
            }
        }

        public void visitFieldInsn(int opcode, String owner, String name, String desc)
        {
            checkName(owner);
            checkDesc(desc);
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc)
        {
            checkName(owner);
            checkMethodDesc(desc);
        }

        public void visitMultiANewArrayInsn(String desc, int dims)
        {
            checkDesc(desc);
        }

        public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
        {
            checkName(type);
        }

        public void visitLocalVariable(String name, String desc, Label start, Label end, int index)
        {
            checkDesc(desc);
        }

        // TODO: attributes
    }
}

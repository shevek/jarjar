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

import java.io.*;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.attrs.Attributes;

abstract class JarTransformer
implements JarProcessor
{
    protected ClassTransformer t;
    
    public boolean process(EntryStruct struct) throws IOException {
        if (struct.name.endsWith(".class")) {
            // System.err.println("processing " + struct.name);
            ClassReader reader = new ClassReader(struct.in);
            struct.in.close();
            GetNameClassWriter w = new GetNameClassWriter(true);
            t.setTarget(w);
            reader.accept(t, Attributes.getDefaultAttributes(), false);
            struct.in = new ByteArrayInputStream(w.toByteArray());
            struct.name = pathFromName(w.getClassName());
        }
        return true;
    }

    private static String pathFromName(String className) {
        return className.replace('.', '/') + ".class";
    }
}

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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.*;

class DepFindVisitor extends RemappingClassAdapter
{
    public DepFindVisitor(Map classes, Object source, DepHandler handler) throws IOException {
        super(new EmptyVisitor(), new DepFindRemapper(classes, source, handler));
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ((DepFindRemapper)remapper).setClassName(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private static class DepFindRemapper extends Remapper
    {
        private final Map classes;
        private final String source;
        private final DepHandler handler;
        private PathClass curPathClass;

        public DepFindRemapper(Map classes, Object source, DepHandler handler) throws IOException {
            this.classes = classes;
            this.source = getSourceName(source);
            this.handler = handler;
        }

        public void setClassName(String name) {
            curPathClass = new PathClass(source, name);
        }

        protected String map(String key) {
            try {
                if (classes.containsKey(key)) {
                    String otherSource = getSourceName(classes.get(key));
                    if (!source.equals(otherSource)) {
                        // TODO: some escape mechanism?
                        handler.handle(curPathClass, new PathClass(otherSource, key));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
            return null;
        }
    }

    private static String getSourceName(Object source) throws IOException {
        if (source instanceof ZipFile) {
            return ((ZipFile)source).getName();
        } else {
            return ((File)source).getCanonicalPath();
        }
    }
}

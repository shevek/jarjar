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
import org.objectweb.asm.*;

class StringDumper
{
    public StringDumper() {
    }

    public void run(String classPath, final PrintWriter pw) throws IOException {
        try {
            StringVisitor sv = new StringVisitor() {
                private String className;
                private boolean needName;
                public void visitStart(String className) {
                    this.className = className;
                    needName = true;
                }
                public void visitString(String value, int line) {
                    if (value.length() > 0) {
                        if (needName) {
                            pw.println(className.replace('/', '.'));
                            needName = false;
                        }
                        pw.print("\t");
                        if (line >= 0)
                            pw.print(line + ": ");
                        pw.print(IoUtils.escapeStringLiteral(value));
                        pw.println();
                    }
                }
                public void visitEnd() {
                    pw.flush();
                }
            };
            StringReader stringReader = new StringReader(sv);
            ClassPathIterator cp = new ClassPathIterator(classPath);
            while (cp.hasNext()) {
                new ClassReader(cp.getInputStream(cp.next())).accept(stringReader, false);
            }
        } catch (RuntimeIOException e) {
            throw (IOException)e.getCause();
        }
    }
}

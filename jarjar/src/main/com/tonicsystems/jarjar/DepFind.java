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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class DepFind
{
    private File curDir = new File(System.getProperty("user.dir"));

    public void setCurrentDirectory(File curDir) {
        this.curDir = curDir;
    }

    public void run(String from, String to, DepHandler handler) throws IOException {
        try {
            ClassHeaderReader header = new ClassHeaderReader();
            Map classes = new HashMap();
            ClassPathIterator cp = new ClassPathIterator(curDir, to);
            while (cp.hasNext()) {
                Object cls = cp.next();
                header.read(cp.getInputStream(cls));
                classes.put(header.getClassName(), cp.getSource(cls));
            }
            cp.close();

            handler.handleStart();
            cp = new ClassPathIterator(curDir, from);
            while (cp.hasNext()) {
                Object cls = cp.next();
                Object source = cp.getSource(cls);
                new ClassReader(cp.getInputStream(cls)).accept(new DepFindVisitor(classes, source, handler),
                                                               ClassReader.SKIP_DEBUG);
            }
            cp.close();
            handler.handleEnd();
        } catch (RuntimeIOException e) {
            throw (IOException)e.getCause();
        }
    }
}

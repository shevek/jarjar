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
import java.util.*;
import java.util.zip.ZipEntry;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import net.sf.cglib.core.TypeUtils;
import net.sf.cglib.transform.NullClassVisitor;

public class DepFind
{
    public static void main(String[] args)
    throws Exception
    {
        try {
            new DepFind(args);
        } catch (WrappedIOException e) {
            throw (IOException)e.getCause();
        }
    }

    private DepFind(String[] args)
    throws Exception
    {
        if (args.length != 2) {
            System.err.println("Syntax: java com.tonicsystems.jarjar.DepFind <find-classpath> <source-classpath>");
            System.exit(1);
        }

        final Set names = new HashSet();
        ClassVisitor gatherNamesVisitor = new NullClassVisitor() {
            public void visit(int access, String name, String superName, String[] interfaces, String sourceFile) {
                names.add(name);
            }
        };

        File curDir = new File(System.getProperty("user.dir"));

        ClassPathIterator cp = new ClassPathIterator(curDir, args[0]);
        while (cp.hasNext()) {
            Object cls = cp.next();
            // System.err.println("d0: " + cls);
            new ClassReader(cp.getInputStream(cls)).accept(gatherNamesVisitor, true);
        }
        
        DepFindVisitor depFind = new DepFindVisitor(names);
        cp = new ClassPathIterator(curDir, args[1]);
        while (cp.hasNext()) {
            try {
                Object cls = cp.next();
                // System.err.println("d1: " + cls);
                new ClassReader(cp.getInputStream(cls)).accept(depFind, true);
            } catch (DepFindException e) {
                System.out.println(e.getClassName() + "\t =>  " + e.getDependency());
            }
        }
    }
}

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
import java.util.Enumeration;
import java.util.jar.*;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.transform.ClassReaderGenerator;
import net.sf.cglib.transform.TransformingClassGenerator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class DepKill
{
    public static void main(String[] args)
    throws Exception
    {
        // long t = System.currentTimeMillis();
        new DepKill(args);
        // System.err.println("took " + (System.currentTimeMillis() - t) + " ms");
    }

    private DepKill(String[] args)
    throws Exception
    {
        if (args.length < 2) {
            System.err.println("Syntax: java com.tonicsystems.jarjar.DepKill <input-jar> <package-name> [<package-name> ...] > <output-jar>");
            System.exit(1);
        }
            
        String[] packageNames = new String[args.length - 1];
        System.arraycopy(args, 1, packageNames, 0, packageNames.length);

        
        DepKillTransformer t = new DepKillTransformer(packageNames);
        JarFile jar = new JarFile(args[0]);
        JarOutputStream out = new JarOutputStream(new BufferedOutputStream(System.out));

        byte[] buf = new byte[0x2000];
        
        for (Enumeration e = jar.entries(); e.hasMoreElements();) {
            JarEntry entry = (JarEntry)e.nextElement();
            InputStream in = jar.getInputStream(entry);

            entry.setCompressedSize(-1);
            out.putNextEntry(entry);
            
            if (entry.getName().endsWith(".class")) {
                // System.err.println("processing " + entry.getName());

                ClassReader reader = new ClassReader(in);
                in.close();
                
                int[] version = reader.getVersion();
                ClassWriter w = new DebuggingClassWriter(true, version[0], version[1]);
                new TransformingClassGenerator(new ClassReaderGenerator(reader, null, false), t).generateClass(w);
                out.write(w.toByteArray());
            
            } else {
                pipe(in, out, buf);
            }
        }
        jar.close();
        out.close();
    }

    private static void pipe(InputStream in, OutputStream out, byte[] buf)
    throws IOException
    {
        for (;;) {
            int amt = in.read(buf);
            if (amt < 0)
                break;
            out.write(buf, 0, amt);
        }
    }
}

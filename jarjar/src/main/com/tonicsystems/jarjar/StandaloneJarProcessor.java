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

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipOutputStream;
import java.io.*;

class StandaloneJarProcessor
{
    public static void run(File from, File to, JarProcessor proc) throws IOException {
        JarFile in = new JarFile(from);
        JarOutputStream out = new JarOutputStream(new FileOutputStream(to));
        byte[] buf = new byte[0x2000];
        EntryStruct struct = new EntryStruct();
        Enumeration e = in.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry)e.nextElement();
            struct.in = in.getInputStream(entry);
            struct.name = entry.getName();
            struct.time = entry.getTime();
            struct.file = from;
            if (proc.process(struct)) {
                entry = new JarEntry(struct.name);
                entry.setTime(struct.time);
                entry.setCompressedSize(-1);
                out.putNextEntry(entry);
                IoUtils.pipe(struct.in, out, buf);
                struct.in.close();
            }
        }
        out.close();
        out = null;
    }
}

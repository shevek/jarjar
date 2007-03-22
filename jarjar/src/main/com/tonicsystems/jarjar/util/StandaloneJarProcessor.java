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

package com.tonicsystems.jarjar.util;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipOutputStream;
import java.io.*;

public class StandaloneJarProcessor
{
    /*
    public static void run(File from, JarProcessor proc) throws IOException {
        JarFile in = new JarFile(from);
        EntryStruct struct = new EntryStruct();
        Enumeration e = in.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry)e.nextElement();
            struct.in = in.getInputStream(entry);
            try {
                struct.name = entry.getName();
                struct.time = entry.getTime();
                struct.file = from;
                proc.process(struct);
            } finally {
                struct.in.close();
            }
        }
    }
    */
    
    public static void run(File from, File to, JarProcessor proc) throws IOException {
        byte[] buf = new byte[0x2000];
        File tmp = null;
        if (from.equals(to)) {
            tmp = File.createTempFile("jarjar", null);
            copy(from, tmp, buf);
            from = tmp;
        }
        JarFile in = new JarFile(from);
        JarOutputStream out = new JarOutputStream(new FileOutputStream(to));
        try {
            EntryStruct struct = new EntryStruct();
            Enumeration e = in.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = (JarEntry)e.nextElement();
                struct.name = entry.getName();
                struct.time = entry.getTime();
                struct.file = from;
                struct.in = in.getInputStream(entry);
                try {
                    if (proc.process(struct)) {
                        entry = new JarEntry(struct.name);
                        entry.setTime(struct.time);
                        entry.setCompressedSize(-1);
                        out.putNextEntry(entry);
                        pipe(struct.in, out, buf);
                    }
                } finally {
                    struct.in.close();
                }
            }
        } finally {
            out.close();
            if (tmp != null)
                tmp.delete();
        }
    }

    private static void copy(File from, File to, byte[] buf) throws IOException {
        InputStream in = new FileInputStream(from);
        try {
            OutputStream out = new FileOutputStream(to);
            try {
                pipe(in, out, buf);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static void pipe(InputStream is, OutputStream out, byte[] buf) throws IOException {
        for (;;) {
            int amt = is.read(buf);
            if (amt < 0)
                break;
            out.write(buf, 0, amt);
        }
    }
}

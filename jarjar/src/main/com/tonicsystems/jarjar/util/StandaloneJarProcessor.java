/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

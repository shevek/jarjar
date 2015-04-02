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
package com.tonicsystems.jarjar.transform;

import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.jar.JarProcessor;
import com.tonicsystems.jarjar.util.IoUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class JarTransformer {

    public static void run(File outputFile, ClassPath inputPath) throws IOException {
        byte[] buf = new byte[0x2000];

        final File tmpFile = File.createTempFile("jarjar", ".jar");
        JarOutputStream tmpJarStream = new JarOutputStream(new FileOutputStream(tmpFile));

        Set<String> entries = new HashSet<String>();
        try {
            EntryStruct struct = new EntryStruct();
            Enumeration<JarEntry> e = in.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                struct.name = entry.getName();
                struct.time = entry.getTime();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IoUtil.copy(in.getInputStream(entry), baos, buf);
                struct.data = baos.toByteArray();
                if (proc.process(struct) != JarProcessor.Result.DISCARD) {
                    if (entries.add(struct.name)) {
                        entry = new JarEntry(struct.name);
                        entry.setTime(struct.time);
                        entry.setCompressedSize(-1);
                        tmpJarStream.putNextEntry(entry);
                        tmpJarStream.write(struct.data);
                    } else if (struct.name.endsWith("/")) {
                        // TODO(chrisn): log
                    } else {
                        throw new IllegalArgumentException("Duplicate jar entries: " + struct.name);
                    }
                }
            }

        } finally {
            in.close();
            tmpJarStream.close();
        }

        // delete the empty directories
        IoUtil.copyZipWithoutEmptyDirectories(tmpFile, outputFile);
        tmpFile.delete();

    }
}

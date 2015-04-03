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
import com.tonicsystems.jarjar.classpath.ClassPathArchive;
import com.tonicsystems.jarjar.classpath.ClassPathResource;
import com.tonicsystems.jarjar.transform.jar.JarProcessor;
import com.tonicsystems.jarjar.util.IoUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.Nonnull;

public class JarTransformer {

    private final File outputFile;
    private final JarProcessor processor;
    private final byte[] buf = new byte[0x2000];

    public JarTransformer(@Nonnull File outputFile, @Nonnull JarProcessor processor) {
        this.outputFile = outputFile;
        this.processor = processor;
    }

    @Nonnull
    private Transformable newTransformable(@Nonnull ClassPathResource inputResource)
            throws IOException {
        Transformable struct = new Transformable();
        struct.name = inputResource.getName();
        struct.time = inputResource.getLastModifiedTime();

        InputStream in = inputResource.openStream();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtil.copy(in, out, buf);
            struct.data = out.toByteArray();
        } finally {
            in.close();
        }

        return struct;
    }

    public void transform(@Nonnull ClassPath inputPath) throws IOException {

        final File tmpFile = File.createTempFile("jarjar", ".jar");

        SCAN:
        {
            for (ClassPathArchive inputArchive : inputPath) {
                for (ClassPathResource inputResource : inputArchive) {
                    Transformable struct = newTransformable(inputResource);
                    processor.scan(struct);
                }
            }
        }

        OUT:
        {
            JarOutputStream tmpJarStream = new JarOutputStream(new FileOutputStream(tmpFile));
            for (ClassPathArchive inputArchive : inputPath) {
                for (ClassPathResource inputResource : inputArchive) {
                    Transformable struct = newTransformable(inputResource);
                    if (processor.process(struct) == JarProcessor.Result.DISCARD)
                        continue;

                    JarEntry tmpEntry = new JarEntry(struct.name);
                    tmpEntry.setTime(struct.time);
                    tmpEntry.setCompressedSize(-1);
                    tmpJarStream.putNextEntry(tmpEntry);
                    tmpJarStream.write(struct.data);
                }
            }
            tmpJarStream.close();
        }

        // delete the empty directories
        IoUtil.copyZipWithoutEmptyDirectories(tmpFile, outputFile);
        tmpFile.delete();

    }
}

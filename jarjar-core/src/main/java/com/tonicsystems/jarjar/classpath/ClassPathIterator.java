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
package com.tonicsystems.jarjar.classpath;

import com.tonicsystems.jarjar.util.ClassNameUtils;
import com.tonicsystems.jarjar.util.RuntimeIOException;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

public class ClassPathIterator implements Iterator<ClassPathEntry>, Closeable {

    private static final FileFilter JAR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isFile() && ClassNameUtils.hasExtension(file.getName(), ".jar");
        }
    };

    private final Iterator<File> files;
    private ClassPathEntryIterator entries = new EmptyIterator();
    private ClassPathEntry next;

    public ClassPathIterator(@Nonnull File parent, @Nonnull List<? extends File> classPath) throws IOException {
        List<File> files = new ArrayList();
        for (File classPathEntry : classPath) {
            boolean wildcard = false;
            if (classPathEntry.getName().equals("*")) {
                classPathEntry = classPathEntry.getParentFile();
                wildcard = true;
            }
            if (classPathEntry.getPath().contains("*"))
                throw new IllegalArgumentException("Incorrect wildcard usage (non-trailing star): " + classPathEntry);

            if (!classPathEntry.isAbsolute())
                classPathEntry = classPathEntry.getAbsoluteFile();
            if (!classPathEntry.exists())
                throw new IllegalArgumentException("File " + classPathEntry + " does not exist");

            if (wildcard) {
                if (!classPathEntry.isDirectory())
                    throw new IllegalArgumentException("File " + classPathEntry + " + is not a directory");
                files.addAll(Arrays.asList(classPathEntry.listFiles(JAR_FILTER)));
            } else {
                files.add(classPathEntry);
            }
        }
        this.files = files.iterator();
        advance();
    }

    private void advance() throws IOException {
        if (!entries.hasNext()) {
            entries.close();
            if (!files.hasNext()) {
                next = null;
                return;
            }
            File file = files.next();
            if (ClassNameUtils.hasExtension(file.getName(), ".jar")) {
                ZipFile zip = new JarFile(file);
                entries = new ZipIterator(zip);
            } else if (ClassNameUtils.hasExtension(file.getName(), ".zip")) {
                ZipFile zip = new ZipFile(file);
                entries = new ZipIterator(zip);
            } else if (file.isDirectory()) {
                entries = new FileIterator(file);
            } else {
                throw new IllegalArgumentException("Do not know how to handle " + file);
            }
        }

        boolean foundClass = false;
        while (!foundClass && entries.hasNext()) {
            next = entries.next();
            foundClass = ClassNameUtils.isClass(next.getName());
        }
        if (!foundClass) {
            advance();
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public ClassPathEntry next() {
        if (!hasNext())
            throw new NoSuchElementException();
        ClassPathEntry result = next;
        try {
            advance();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /** Closes all zip files opened by this iterator. */
    @Override
    public void close() throws IOException {
        next = null;
    }

    private static abstract class ClassPathEntryIterator implements Iterator<ClassPathEntry>, Closeable {

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class EmptyIterator extends ClassPathEntryIterator {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ClassPathEntry next() {
            throw new NoSuchElementException();
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class ZipIterator extends ClassPathEntryIterator {

        private final ZipFile zip;
        private final Enumeration<? extends ZipEntry> entries;

        ZipIterator(@Nonnull ZipFile zip) {
            this.zip = zip;
            this.entries = zip.entries();
        }

        @Override
        public boolean hasNext() {
            return entries.hasMoreElements();
        }

        @Override
        public ClassPathEntry next() {
            final ZipEntry entry = entries.nextElement();
            return new ClassPathEntry() {
                @Override
                public String getSource() {
                    return zip.getName();
                }

                @Override
                public String getName() {
                    return entry.getName();
                }

                @Override
                public InputStream openStream() throws IOException {
                    return zip.getInputStream(entry);
                }
            };
        }

        @Override
        public void close() throws IOException {
            zip.close();
        }
    }

    private static class FileIterator extends ClassPathEntryIterator {

        @Nonnull
        private static List<File> findClassFiles(List<File> out, File dir) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    findClassFiles(out, file);
                } else if (file.isFile()) {
                    if (ClassNameUtils.isClass(file.getName())) {
                        out.add(file);
                    }
                }
            }
            return out;
        }

        private final File dir;
        private final Iterator<File> entries;

        FileIterator(@Nonnull File dir) {
            this.dir = dir;
            this.entries = findClassFiles(new ArrayList<File>(), dir).iterator();
        }

        @Override
        public boolean hasNext() {
            return entries.hasNext();
        }

        @Override
        public ClassPathEntry next() {
            final File file = entries.next();
            return new ClassPathEntry() {
                public String getSource() throws IOException {
                    return dir.getCanonicalPath();
                }

                public String getName() {
                    return file.getName();
                }

                public InputStream openStream() throws IOException {
                    return new BufferedInputStream(new FileInputStream(file));
                }
            };
        }

        @Override
        public void close() throws IOException {
        }
    }

}

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

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.util.jar.*;

public class ClassPathIterator implements Iterator
{
    private static final FileFilter CLASS_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory() || isClass(file);
        }
    };

    private static final FileFilter JAR_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return hasExtension(getName(file), ".jar");
        }
    };
    
    private final Iterator files;
    private Enumeration entries;
    private Map sources = new HashMap();
    private ZipFile zip;
    private Object next;

    public ClassPathIterator(String classPath) throws IOException {
        this(new File(System.getProperty("user.dir")), classPath, null);
    }
    
    public ClassPathIterator(File parent, String classPath, String delim) throws IOException {
        if (delim == null) {
            delim = System.getProperty("path.separator");
        }
        StringTokenizer st = new StringTokenizer(classPath, delim);
        List fileList = new ArrayList();
        while (st.hasMoreTokens()) {
            String part = (String)st.nextElement();

            boolean wildcard = false;
            if (part.endsWith("/*")) {
                part = part.substring(0, part.length() - 1);
                if (part.indexOf('*') >= 0)
                    throw new IllegalArgumentException("Multiple wildcards are not allowed: " + part);
                wildcard = true;
            } else if (part.indexOf('*') >= 0) {
                throw new IllegalArgumentException("Incorrect wildcard usage: " + part);
            }
                
            File file = new File(part);
            if (!file.isAbsolute())
                file = new File(parent, part);
            if (!file.exists())
                throw new IllegalArgumentException("File " + file + " does not exist");

            if (wildcard) {
                if (!file.isDirectory())
                    throw new IllegalArgumentException("File " + file + " + is not a directory");
                fileList.addAll(findFiles(file, JAR_FILTER, false, new ArrayList()));
            } else {
                fileList.add(file);
            }
        }
        this.files = fileList.iterator();
        advance();
    }

    public boolean hasNext() {
        return next != null;
    }

    public void close() throws IOException {
        if (sources != null) {
            for (Iterator it = sources.values().iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof ZipFile)
                    ((ZipFile)obj).close();
            }
        }
    }
    
    public InputStream getInputStream(Object obj) throws IOException {
        if (obj instanceof ZipEntry) {
            return ((ZipFile)sources.get(obj)).getInputStream((ZipEntry)obj);
        } else {
            return new BufferedInputStream(new FileInputStream((File)obj));
        }
    }

    public Object getSource(Object obj) {
        return sources.get(obj);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws RuntimeIOException
     */
    public Object next() {
        if (!hasNext())
            throw new NoSuchElementException();
        Object result = next;
        try {
            advance();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return result;
    }

    private void advance() throws IOException {
        if (entries == null) {
            if (!files.hasNext()) {
                next = null;
                return;
            }
            zip = null;
            File file = (File)files.next();
            if (hasExtension(getName(file), ".jar")) {
                zip = new JarFile(file);
                entries = zip.entries();
            } else if (hasExtension(getName(file), ".zip")) {
                zip = new ZipFile(file);
                entries = zip.entries();
            } else if (file.isDirectory()) {
                // TODO: could lazily recurse for performance
                List classes = findFiles(file, CLASS_FILTER, true, new ArrayList());
                for (Iterator it = classes.iterator(); it.hasNext();) {
                    sources.put(it.next(), file);
                }
                entries = Collections.enumeration(classes);
            } else {
                throw new IllegalArgumentException("Do not know how to handle " + file);
            }
        }

        boolean foundClass = false;
        while (entries.hasMoreElements()) {
            next = entries.nextElement();
            if (foundClass = isClass(next)) {
                if (zip != null)
                    sources.put(next, zip);
                break;
            }
        }
        if (!foundClass) {
            entries = null;
            advance();
        }
    }

    private static List findFiles(File dir, FileFilter filter, boolean recurse, List collect) {
        File[] files = dir.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
            if (recurse && files[i].isDirectory()) {
                findFiles(files[i], filter, recurse, collect);
            } else {
                collect.add(files[i]);
            }
        }
        return collect;
    }

    private static boolean isClass(Object obj) {
        // TODO: check magic number?
        return hasExtension(getName(obj), ".class");
    }

    private static String getName(Object obj) {
        return (obj instanceof ZipEntry) ? ((ZipEntry)obj).getName() : ((File)obj).getName();
    }
    
    private static boolean hasExtension(String name, String ext) {
        if (name.length() <  ext.length())
            return false;
        String actual = name.substring(name.length() - ext.length());
        return actual.equals(ext) || actual.equals(ext.toUpperCase());
    }
}

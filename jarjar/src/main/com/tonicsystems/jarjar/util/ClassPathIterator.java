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

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.util.jar.*;

public class ClassPathIterator implements Iterator
{
    private static final FileFilter CLASS_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory() || hasExtension(getName(file), ".class");
        }
    };

    private static final FileFilter JAR_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return hasExtension(getName(file), ".jar");
        }
    };
    
    private Iterator files;
    private Enumeration entries;
    private Map sources = new HashMap();
    private ZipFile zip;
    private Object next;

    public ClassPathIterator(String classPath) {
        this(new File(System.getProperty("user.dir")), classPath);
    }
    
    public ClassPathIterator(File parent, String classPath) {
        StringTokenizer st = new StringTokenizer(classPath, System.getProperty("path.separator"));
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

    public Object next() {
        if (!hasNext())
            throw new NoSuchElementException();
        Object result = next;
        advance();
        return result;
    }

    private void advance() {
        try {
            
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
                if (foundClass = hasExtension(getName(next), ".class")) {
                    if (zip != null)
                        sources.put(next, zip);
                    break;
                }
            }
            if (!foundClass) {
                entries = null;
                advance();
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
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

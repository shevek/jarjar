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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.transform.ClassReaderGenerator;
import net.sf.cglib.transform.TransformingClassGenerator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;

public class JarJarTask extends Jar
{
    private static final String GENERATED_PATH = pathFromName(StringTransformer.GENERATED_NAME);
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    private ArrayList ruleList = new ArrayList();
    private ArrayList zapList = new ArrayList();
    private RulesImpl rules;
    private PackageTransformer t;
    private boolean verbose;
    private Map fileToTransformer = new HashMap();
    private Map transformerToTransformerName = new HashMap();
    private Map classNameToTransformer = new HashMap();
    private Map fileToTransformerBytes = new HashMap();
    private Map classNameToClassName = new HashMap();
    private byte[] buf = new byte[0x2000];

    private static String pathFromName(String className) {
        return className.replace('.', '/') + ".class";
    }

    private static String nameFromPath(String path) {
        String name = path.replace('/', '.');
        return name.substring(0, name.length() - 6);
    }

    public void addConfiguredRule(Rule rule) {
        if (rule.getPattern() == null || rule.getResult() == null)
            throw new IllegalArgumentException("The <rule> element requires both \"pattern\" and \"result\" attributes.");
        ruleList.add(rule);
    }

    public void addConfiguredZap(Zap zap) {
        if (zap.getPattern() == null)
            throw new IllegalArgumentException("The <zap> element requires a \"pattern\" attribute.");
        zapList.add(new Wildcard(zap.getPattern(), ""));
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void execute() throws BuildException {
        setFilesonly(true);
        addManifestEntry();
        t = new PackageTransformer(rules = new RulesImpl(ruleList, verbose));
        super.execute();
    }

    private void addManifestEntry() throws BuildException {
        try {
            org.apache.tools.ant.taskdefs.Manifest manifest = new org.apache.tools.ant.taskdefs.Manifest();
            manifest.addConfiguredAttribute(new Attribute(StringTransformer.MANIFEST_ATTRIBUTE, StringTransformer.GENERATED_NAME));
            addConfiguredManifest(manifest);
        } catch (ManifestException e) {
            throw new BuildException(e);
        }
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath, int mode) throws IOException {
        // ignore
    }
    
    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode) throws IOException {
        if (vPath.equals(GENERATED_PATH)) {
            fileToTransformerBytes.put(fromArchive, toByteArray(is, buf));
            return;
        }
        if (fromArchive != null && vPath.equalsIgnoreCase(MANIFEST_PATH)) {
            // TODO: merge manifests?
            if (verbose)
                System.err.println("Ignored " + vPath);
            return;
        }
        if (vPath.endsWith(".class")) {
            try {
                String oldClassName = nameFromPath(vPath);
                if (zap("L" + vPath.substring(0, vPath.length() - 6) + ";")) {
                    if (verbose)
                        System.err.println("Zapping " + oldClassName);
                    return;
                }
                WrappedTransformer st = null;
                if (fromArchive != null) {
                    synchronized (this) {
                        if (fileToTransformer.containsKey(fromArchive)) {
                            st = (WrappedTransformer)fileToTransformer.get(fromArchive);
                        } else {
                            fileToTransformer.put(fromArchive, st = getStringTransformer(fromArchive));
                            if (st != null)
                                transformerToTransformerName.put(st, st.inner.getClass().getName());
                        }
                    }
                }
                rules.setStringTransformer(st);
                DebuggingClassWriter w = new DebuggingClassWriter(false);
                new TransformingClassGenerator(new ClassReaderGenerator(new ClassReader(is), false), t).generateClass(w);

                String newClassName = w.getClassName();
                if (classNameToClassName.containsKey(oldClassName))
                    throw new BuildException("Error renaming " + oldClassName + ": already renamed " + classNameToClassName.get(oldClassName) + " to " + newClassName);
                if (st != null)
                    classNameToTransformer.put(oldClassName, st);
                classNameToClassName.put(oldClassName, newClassName);
                if (verbose && !oldClassName.equals(newClassName))
                    System.err.println("Renamed " + oldClassName + " -> " + newClassName);

                vPath = pathFromName(newClassName);
                is = new ByteArrayInputStream(w.toByteArray());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } else {
            String oldPath = vPath;
            vPath = rules.fixPath(vPath);
            if (verbose) {
                if (vPath.equals(oldPath)) {
                    System.err.println("Skipped " + vPath);
                } else {
                    System.err.println("Renamed " + oldPath + " -> " + vPath);
                }
            }
       }
        super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode);
    }

    private WrappedTransformer getStringTransformer(File fromArchive) throws Exception {
        JarFile jarFile = new JarFile(fromArchive);
        try {
            Manifest manifest = jarFile.getManifest();
            final String name = manifest.getMainAttributes().getValue(StringTransformer.MANIFEST_ATTRIBUTE);
            if (name != null) {
                URL url = new URL("jar:file:" + fromArchive.getAbsolutePath() + "!/");
                ClassLoader cl = new URLClassLoader(new URL[]{ url }, getClass().getClassLoader());
                try {
                    if (verbose)
                        System.err.println("Loading " + name + " from " + fromArchive);
                    Class c = cl.loadClass(name);
                    return new WrappedTransformer((StringTransformer)c.newInstance());
                } catch (ClassNotFoundException e) {
                    // don't worry if generated transformer is missing, we have to write the manifest
                    // before we know if we'll really need it
                    if (!name.equals(StringTransformer.GENERATED_NAME)) {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            jarFile.close();
        }
        return null;
    }

    private class WrappedTransformer implements StringTransformer
    {
        private StringTransformer inner;
        
        public WrappedTransformer(StringTransformer inner) {
            if (inner == null)
                throw new RuntimeException("inner is null!");
            this.inner = inner;
        }

        public String transform(String value, String className, StringTransformer def) {
            String newValue = inner.transform(value, className, def);
            if (newValue == null)
                throw new IllegalStateException("StringTransformer cannot return null");
            return newValue;
        }
    }
      
    private static byte[] toByteArray(InputStream is, byte[] buf) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pipe(is, baos, buf);
        return baos.toByteArray();
    }

    private static void pipe(InputStream is, OutputStream out, byte[] buf) throws IOException {
        for (;;) {
            int amt = is.read(buf);
            if (amt < 0)
                break;
            out.write(buf, 0, amt);
        }
    }

    private boolean zap(String desc) {
        // TODO: optimize
        for (Iterator it = zapList.iterator(); it.hasNext();) {
            if (((Wildcard)it.next()).matches(desc, Wildcard.STYLE_DESC)) {
                return true;
            }
        }
        return false;
    }

    protected void finalizeZipOutputStream(ZipOutputStream zOut) throws IOException, BuildException {
        if (!classNameToTransformer.isEmpty()) {
            DebuggingClassWriter w = new DebuggingClassWriter(true);
            try {
                new StringTransformerGenerator(rules,
                                               classNameToClassName,
                                               classNameToTransformer,
                                               fileToTransformer,
                                               fileToTransformerBytes,
                                               transformerToTransformerName).generateClass(w);
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new BuildException(e);
            }
            super.zipFile(new ByteArrayInputStream(w.toByteArray()),
                          zOut,
                          GENERATED_PATH,
                          System.currentTimeMillis(),
                          null,
                          ZipFileSet.DEFAULT_FILE_MODE);
            super.finalizeZipOutputStream(zOut);
        }
    }

    public void reset()
    {
        super.reset();
        cleanHelper();
    }

    protected void cleanUp()
    {
        super.cleanUp();
        cleanHelper();
    }

    private void cleanHelper()
    {
        verbose = false;
        ruleList.clear();
        zapList.clear();
        classNameToTransformer.clear();
        transformerToTransformerName.clear();
        fileToTransformer.clear();
        fileToTransformerBytes.clear();
        classNameToClassName.clear();
    }
}

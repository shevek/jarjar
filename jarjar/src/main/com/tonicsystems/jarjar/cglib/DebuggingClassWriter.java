/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tonicsystems.jarjar.cglib;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader;
import java.io.*;

public class DebuggingClassWriter extends ClassWriter {
    
    public static final String DEBUG_LOCATION_PROPERTY = "cglib.debugLocation";
    
    private static String debugLocation;
    private String className;
    private String superName;
    
    static {
        debugLocation = System.getProperty(DEBUG_LOCATION_PROPERTY);
        if (debugLocation != null)
            System.err.println("CGLIB debugging enabled, writing to '" + debugLocation + "'");
    }
    
    public DebuggingClassWriter(boolean computeMaxs) {
        super(computeMaxs);
    }

    public void visit(int version, int access, String name, String superName, String[] interfaces, String sourceFile) {
        className = name.replace('/', '.');
        this.superName = superName.replace('/', '.');
        super.visit(version, access, name, superName, interfaces, sourceFile);
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getSuperName() {
        return superName;
    }
    
    public byte[] toByteArray() {
        
      return (byte[]) java.security.AccessController.doPrivileged(
        new java.security.PrivilegedAction() {
            public Object run() {
                
                
                byte[] b = DebuggingClassWriter.super.toByteArray();
                if (debugLocation != null) {
                    String dirs = className.replace('.', File.separatorChar);
                    try {
                        new File(debugLocation + File.separatorChar + dirs).getParentFile().mkdirs();
                        
                        File file = new File(new File(debugLocation), dirs + ".class");
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            out.write(b);
                        } finally {
                            out.close();
                        }
                    } catch (IOException e) {
                        throw new CodeGenerationException(e);
                    }
                }
                return b;
             }  
            });
            
        }
    }

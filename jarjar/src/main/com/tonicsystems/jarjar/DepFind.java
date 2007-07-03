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

package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class DepFind
{
    private File curDir = new File(System.getProperty("user.dir"));

    public void setCurrentDirectory(File curDir) {
        this.curDir = curDir;
    }

    public void run(String from, String to, DepHandler handler) throws IOException {
        try {
            ClassHeaderReader header = new ClassHeaderReader();
            Map classes = new HashMap();
            ClassPathIterator cp = new ClassPathIterator(curDir, to);
            while (cp.hasNext()) {
                Object cls = cp.next();
                try {
                    header.read(cp.getInputStream(cls));
                    classes.put(header.getClassName(), cp.getSource(cls));
                } catch (ClassFormatError e) {
                    // TODO: log?
                }
            }
            cp.close();

            handler.handleStart();
            cp = new ClassPathIterator(curDir, from);
            while (cp.hasNext()) {
                Object cls = cp.next();
                Object source = cp.getSource(cls);
                try {
                IoUtils.readClass(cp.getInputStream(cls))
                    .accept(new DepFindVisitor(classes, source, handler), ClassReader.SKIP_DEBUG);
                } catch (ClassFormatError e) {
                    // TODO: log?
                }
            }
            cp.close();
            handler.handleEnd();
        } catch (RuntimeIOException e) {
            throw (IOException)e.getCause();
        }
    }
}

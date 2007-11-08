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
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.*;

class DepFindVisitor extends RemappingClassAdapter
{
    public DepFindVisitor(Map classes, Object source, DepHandler handler) throws IOException {
        super(new EmptyVisitor(), new DepFindRemapper(classes, source, handler));
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ((DepFindRemapper)remapper).setClassName(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private static class DepFindRemapper extends Remapper
    {
        private final Map classes;
        private final String source;
        private final DepHandler handler;
        private PathClass curPathClass;

        public DepFindRemapper(Map classes, Object source, DepHandler handler) throws IOException {
            this.classes = classes;
            this.source = getSourceName(source);
            this.handler = handler;
        }

        public void setClassName(String name) {
            curPathClass = new PathClass(source, name);
        }

        public String map(String key) {
            try {
                if (classes.containsKey(key)) {
                    String otherSource = getSourceName(classes.get(key));
                    if (!source.equals(otherSource)) {
                        // TODO: some escape mechanism?
                        handler.handle(curPathClass, new PathClass(otherSource, key));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
            return null;
        }
    }

    private static String getSourceName(Object source) throws IOException {
        if (source instanceof ZipFile) {
            return ((ZipFile)source).getName();
        } else {
            return ((File)source).getCanonicalPath();
        }
    }
}

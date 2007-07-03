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
import org.objectweb.asm.*;

class StringDumper
{
    public StringDumper() {
    }

    public void run(String classPath, PrintWriter pw) throws IOException {
        StringReader stringReader = new StringReader(new DumpStringVisitor(pw));
        ClassPathIterator cp = new ClassPathIterator(classPath);
        try {
            while (cp.hasNext()) {
                try {
                    IoUtils.readClass(cp.getInputStream(cp.next())).accept(stringReader, ClassReader.SKIP_DEBUG);
                } catch (ClassFormatError e) {
                    // TODO: log?
                }
            }
        } catch (RuntimeIOException e) {
            throw (IOException)e.getCause();
        }
    }

    private static class DumpStringVisitor
    implements StringVisitor
    {
        private final PrintWriter pw;
        private String className;
        private boolean needName;

        public DumpStringVisitor(PrintWriter pw) {
            this.pw = pw;
        }

        public void visitStart(String className) {
            this.className = className;
            needName = true;
        }

        public void visitString(String value, int line) {
            if (value.length() > 0) {
                if (needName) {
                    pw.println(className.replace('/', '.'));
                    needName = false;
                }
                pw.print("\t");
                if (line >= 0)
                    pw.print(line + ": ");
                pw.print(IoUtils.escapeStringLiteral(value));
                pw.println();
            }
        }

        public void visitEnd() {
            pw.flush();
        }
    };
}

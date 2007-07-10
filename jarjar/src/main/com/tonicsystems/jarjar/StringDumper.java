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
        StringReader stringReader = new DumpStringReader(pw);
        ClassPathIterator cp = new ClassPathIterator(classPath);
        try {
            while (cp.hasNext()) {
                try {
                    IoUtils.readClass(cp.getInputStream(cp.next())).accept(stringReader, 0);
                } catch (ClassFormatError e) {
                    // TODO: log?
                }
                pw.flush();
            }
        } catch (RuntimeIOException e) {
            throw (IOException)e.getCause();
        }
    }

    private static class DumpStringReader extends StringReader
    {
        private final PrintWriter pw;
        private String className;

        public DumpStringReader(PrintWriter pw) {
            this.pw = pw;
        }

        public void visitString(String className, String value, int line) {
            if (value.length() > 0) {
                if (!className.equals(this.className)) {
                    this.className = className;
                    pw.println(className.replace('/', '.'));
                }
                pw.print("\t");
                if (line >= 0)
                    pw.print(line + ": ");
                pw.print(IoUtils.escapeStringLiteral(value));
                pw.println();
            }
        }
    };
}

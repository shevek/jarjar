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

import java.io.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

abstract public class AntJarProcessor extends Jar
{
    private EntryStruct struct = new EntryStruct();
    private JarProcessor proc;
    private byte[] buf = new byte[0x2000];

    protected boolean verbose;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    abstract public void execute() throws BuildException;

    public void execute(JarProcessor proc) throws BuildException {
        setFilesonly(true);
        this.proc = proc;
        super.execute();
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath, int mode) throws IOException {
        // ignore
    }

    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                          long lastModified, File fromArchive, int mode) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtil.pipe(is, baos, buf);
        struct.data = baos.toByteArray();
        struct.name = vPath;
        struct.time = lastModified;
        struct.file = fromArchive;
        if (proc.process(struct)) {
            if (mode == 0)
                mode = ZipFileSet.DEFAULT_FILE_MODE;
            super.zipFile(new ByteArrayInputStream(struct.data),
                          zOut, struct.name, struct.time, struct.file, mode);
        }
    }

    public void reset() {
        super.reset();
        cleanHelper();
    }

    protected void cleanUp() {
        super.cleanUp();
        cleanHelper();
    }

    protected void cleanHelper() {
        verbose = false;
    }
}

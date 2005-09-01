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

import com.tonicsystems.jarjar.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.zip.ZipOutputStream;

abstract public class AntJarProcessor extends Jar
{
    private EntryStruct struct = new EntryStruct();
    private JarProcessor proc;
    protected boolean verbose;

    abstract protected JarProcessor getJarProcessor();

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void execute() throws BuildException {
        setFilesonly(true);
        proc = getJarProcessor();
        super.execute();
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath, int mode) throws IOException {
        // ignore
    }
    
    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode) throws IOException {
        struct.in = is;
        struct.name = vPath;
        struct.time = lastModified;
        struct.file = fromArchive;
        if (proc.process(struct))
            super.zipFile(struct.in, zOut, struct.name, struct.time, struct.file, mode);
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

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

import com.tonicsystems.jarjar.util.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

public class JarJarMojo extends AbstractMojo
{
    private File fromJar;
    private File toJar;
    private File rulesFile;
    private String rules;
    private boolean verbose;
    
    public void execute() throws MojoExecutionException {
        if (!((rulesFile == null || !rulesFile.exists()) ^ (rules == null)))
            throw new MojoExecutionException("Exactly one of rules or rulesFile is required");

        try {
            List patterns = null;
            if (patterns != null) {
                patterns = RulesFileParser.parse(rules);
            } else {
                patterns = RulesFileParser.parse(rulesFile);
            }

            JarProcessor proc = new MainProcessor(patterns, verbose);
            StandaloneJarProcessor.run(fromJar, toJar, proc);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

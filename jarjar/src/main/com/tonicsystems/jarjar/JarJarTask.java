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
import java.util.*;

public class JarJarTask extends AntJarProcessor
{
    private List patterns = new ArrayList();

    public void addConfiguredRule(Rule rule) {
        if (rule.getPattern() == null || rule.getResult() == null)
            throw new IllegalArgumentException("The <rule> element requires both \"pattern\" and \"result\" attributes.");
        patterns.add(rule);
    }

    public void addConfiguredZap(Zap zap) {
        if (zap.getPattern() == null)
            throw new IllegalArgumentException("The <zap> element requires a \"pattern\" attribute.");
        patterns.add(zap);
    }

    public void addConfiguredKill(Kill kill) {
        if (kill.getPattern() == null)
            throw new IllegalArgumentException("The <kill> element requires a \"pattern\" attribute.");
        patterns.add(kill);
    }
    
    protected JarProcessor getJarProcessor() {
        return new MainProcessor(patterns, verbose, false);
    }

    protected void cleanHelper() {
        super.cleanHelper();
        patterns.clear();
    }
}

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

import java.util.*;

public class JarJarTask extends AntJarProcessor
{
    private ArrayList ruleList = new ArrayList();
    private ArrayList zapList = new ArrayList();

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

    protected JarProcessor getJarProcessor()
    {
        return new MainProcessor(ruleList, zapList, verbose);
    }

    protected void cleanHelper() {
        super.cleanHelper();
        ruleList.clear();
        zapList.clear();
    }
}

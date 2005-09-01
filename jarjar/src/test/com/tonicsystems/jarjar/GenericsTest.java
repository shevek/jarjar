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
import junit.framework.*;
import java.util.*;
import org.objectweb.asm.ClassReader;

public class GenericsTest
extends TestCase
{
    public void testTransform() throws Exception {
         Rule rule = new Rule();
         rule.setPattern("java.lang.String");
         rule.setResult("com.tonicsystems.String");
         Rules rules = new RulesImpl(Arrays.asList(new Object[]{ rule }), false);
         PackageTransformer t = new PackageTransformer(rules);
         t.setTarget(NullClassVisitor.getInstance());
         ClassReader reader = new ClassReader(getClass().getResourceAsStream("/Generics.class"));
         reader.accept(t, false);
    }

    public GenericsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(GenericsTest.class);
    }
}

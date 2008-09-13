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
import junit.framework.*;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

public class GenericsTest
extends TestCase
{
    public void testTransform() throws Exception {
         Rule rule = new Rule();
         rule.setPattern("java.lang.String");
         rule.setResult("com.tonicsystems.String");
         ClassTransformer t = new RemappingClassTransformer(new PackageRemapper(Arrays.asList(rule), false));
         t.setTarget(new EmptyVisitor());
         ClassReader reader = new ClassReader(getClass().getResourceAsStream("/Generics.class"));
         reader.accept(t, 0);
    }

    public GenericsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(GenericsTest.class);
    }
}

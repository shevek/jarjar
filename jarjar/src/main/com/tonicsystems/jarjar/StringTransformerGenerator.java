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

import java.io.File;
import java.util.*;
import net.sf.cglib.core.*;
import net.sf.cglib.transform.*;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.CodeAdapter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

class StringTransformerGenerator
{
    private static final Type STRING_TRANSFORMER =
      Type.getType(StringTransformer.class);
    private static final Signature TRANSFORM =
      TypeUtils.parseSignature("String transform(String, String, com.tonicsystems.jarjar.StringTransformer)");
    private static final String NAME_PREFIX = "TRANSFORMER";
    private static final String STATIC_PREFIX = "STATIC";

    private PackageTransformer packageTransformer;
    private Map classNameToTransformer;
    private Map classNameToClassName;
    private Map fileToTransformer;
    private Map fileToTransformerBytes;
    private Map transformerToTransformerName;
    private Map reversedClassNames = new HashMap();
    private int index;
    private int staticIndex;

    public StringTransformerGenerator(final Rules rules,
                                      Map classNameToClassName,
                                      Map classNameToTransformer,
                                      Map fileToTransformer,
                                      Map fileToTransformerBytes,
                                      Map transformerToTransformerName) {
        this.classNameToClassName = classNameToClassName;
        this.classNameToTransformer = classNameToTransformer;
        this.fileToTransformer = fileToTransformer;
        this.fileToTransformerBytes = fileToTransformerBytes;
        this.transformerToTransformerName = transformerToTransformerName;
        CollectionUtils.reverse(classNameToClassName, reversedClassNames);

        packageTransformer = new PackageTransformer(new Rules() {
            public String fixDesc(String desc) {
                return rules.fixDesc(desc);
            }
            public String fixName(String name) {
                return rules.fixName(name);
            }
            public String fixPath(String name) {
                throw new RuntimeException("impossible");
            }
            public String fixMethodDesc(String desc) {
                return rules.fixMethodDesc(desc);
            }
            public String fixString(String className, String value) {
                return value;
            }
            public Attribute fixAttribute(Attribute attrs) {
                return rules.fixAttribute(attrs);
            }
        });
    }

    public void generateClass(ClassVisitor v) throws Exception {
        ClassEmitter cls = new ClassEmitter(v);
        cls.begin_class(Constants.ACC_PUBLIC,
                        StringTransformer.GENERATED_NAME,
                        null,
                        new Type[]{ STRING_TRANSFORMER },
                        Constants.SOURCE_FILE);
        Map transformerNameToIndex = new HashMap();
        EmitUtils.null_constructor(cls);
        emitShunts(cls, transformerNameToIndex);
        emitStatic(cls, transformerNameToIndex);
        emitTransforms(cls, transformerNameToIndex);
        cls.end_class();
    }

    private void emitShunts(ClassEmitter cls, Map transformerNameToIndex) throws Exception {
        for (Iterator it = fileToTransformer.keySet().iterator(); it.hasNext();) {
            File file = (File)it.next();
            String transformerName = getTransformerName((StringTransformer)fileToTransformer.get(file));
            if (StringTransformer.GENERATED_NAME.equals(transformerName)) {
                int shunted = emitShunt(cls, (byte[])fileToTransformerBytes.get(file));
                transformerNameToIndex.put(transformerName, new Integer(shunted));
            }
        }
    }

    private void emitStatic(ClassEmitter cls, Map transformerNameToIndex) {
        CodeEmitter e = cls.begin_static();
        int c = 0;
        for (int i = 0; i < staticIndex; i++) {
            e.invoke_static_this(getStaticShunt(i));
        }
        for (Iterator it = fileToTransformer.values().iterator(); it.hasNext();) {
            StringTransformer key = (StringTransformer)it.next();
            String transformerName = getTransformerName(key);
            if (transformerName != null && !StringTransformer.GENERATED_NAME.equals(transformerName)) {
                cls.declare_field(Constants.PRIVATE_FINAL_STATIC,
                                  getField(index),
                                  STRING_TRANSFORMER,
                                  null,
                                  null);
                Type type = TypeUtils.getType(transformerName);
                e.new_instance(type);
                e.dup();
                e.invoke_constructor(type);
                e.putfield(getField(index));
                transformerNameToIndex.put(transformerName, new Integer(index));
                index++;
            }
        }
        e.end_method();
    }

    private int emitShunt(ClassEmitter cls, byte[] bytes) throws Exception {
        final int[] index = { staticIndex };
        final Map renamed = new HashMap();
        new ClassReader(bytes).accept(new NullClassVisitor() {
            public CodeVisitor visitMethod(int access, final String name, String desc, String[] exceptions, Attribute attrs) {
                if (name.equals("<clinit>") || name.startsWith(STATIC_PREFIX)) {
                    renamed.put(name, getStaticShunt(index[0]++).getName());
                }
                return null;
            }
        }, true);
        staticIndex = index[0];
        ShuntTransformer shunt = new ShuntTransformer(renamed);
        new TransformingClassGenerator(new ClassReaderGenerator(new ClassReader(bytes), false), shunt).generateClass(cls);
        return shunt.shunted;
    }
    
    private void emitTransforms(ClassEmitter cls, final Map transformerNameToIndex) {
        final CodeEmitter e = cls.begin_method(Constants.ACC_PUBLIC, TRANSFORM, null, null);
        e.load_arg(1);
        EmitUtils.string_switch(e, getClassNames(), Constants.SWITCH_STYLE_HASHONLY, new ObjectSwitchCallback() {
            public void processCase(Object key, Label end) {
                String transformerName = getTransformerName((String)key);
                int index = ((Integer)transformerNameToIndex.get(transformerName)).intValue();
                if (transformerName.equals(StringTransformer.GENERATED_NAME)) {
                    e.load_this();
                    e.load_arg(0);
                    e.push((String)reversedClassNames.get((String)key));
                    e.load_arg(2);
                    e.invoke_virtual_this(getShunt(index));
                    e.return_value();
                } else {
                    e.getfield(getField(index));
                    e.load_arg(0);
                    e.push((String)reversedClassNames.get((String)key));
                    e.load_arg(2);
                    e.invoke_interface(STRING_TRANSFORMER, TRANSFORM);
                    e.return_value();
                }
            }
            public void processDefault() {
                e.load_arg(2);
            }
        });
        e.load_args();
        e.invoke_interface(STRING_TRANSFORMER, TRANSFORM);
        e.return_value();
        e.end_method();
    }

    private String getField(int c) {
        return NAME_PREFIX + c;
    }

    private Signature getShunt(int c) {
        return new Signature(NAME_PREFIX + c, TRANSFORM.getDescriptor());
    }

    private Signature getStaticShunt(int c) {
        return new Signature(STATIC_PREFIX + c, "()V");
    }

    private String getTransformerName(String className) {
        return getTransformerName((StringTransformer)classNameToTransformer.get(reversedClassNames.get(className)));
    }

    private String getTransformerName(StringTransformer t) {
        if (t == null)
            return null;
        String name = (String)transformerToTransformerName.get(t);
        if (name.equals(StringTransformer.GENERATED_NAME)) {
            return name;
        } else {
            return (String)classNameToClassName.get(name);
        }
    }

    private String[] getClassNames() {
        List names = new ArrayList(classNameToTransformer.size());
        for (Iterator it = classNameToTransformer.keySet().iterator(); it.hasNext();) {
            names.add(classNameToClassName.get((String)it.next()));
        }
        return (String[])names.toArray(new String[names.size()]);
    }

    private class ShuntTransformer extends AbstractClassTransformer {
        private Map renamed;
        private int shunted;
        
        public ShuntTransformer(Map renamed) {
            this.renamed = renamed;
        }

        public void visit(int access, String name, String superName, String[] interfaces, String sourceFile) {
            // ignore
        }

        public void visitField(int access, String name, String desc, Object value, Attribute attrs) {
            updateIndex(name);
            super.visitField(access, name, desc, value, attrs);
        }

        public CodeVisitor visitMethod(int access, final String name, String desc, String[] exceptions, Attribute attrs) {
            if (name.equals("<init>"))
                return null;
            if (name.equals("<clinit>") || name.startsWith(STATIC_PREFIX)) {
                packageTransformer.setTarget(cv);
                return new CodeAdapter(packageTransformer.visitMethod(access, (String)renamed.get(name), desc, exceptions, attrs)) {
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (opcode == Constants.INVOKESTATIC && name.startsWith(STATIC_PREFIX))
                            name = (String)renamed.get(name);
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }
                };
            }
            if (name.equals("transform")) {
                shunted = index++;
                return super.visitMethod(access, getShunt(shunted).getName(), desc, exceptions, attrs);
            } else {
                updateIndex(name);
                return super.visitMethod(access, name, desc, exceptions, attrs);
            }
        }
    }

    private void updateIndex(String name) {
        if (name.startsWith(NAME_PREFIX))
            index = Math.max(index, Integer.parseInt(name.substring(NAME_PREFIX.length())) + 1);
    }
}

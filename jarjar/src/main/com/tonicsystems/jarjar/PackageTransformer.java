package com.tonicsystems.jarjar;

import net.sf.cglib.transform.AbstractClassTransformer;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.CodeAdapter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Label;

class PackageTransformer
extends AbstractClassTransformer
{
    private Rules rules;
    private String className;
    private boolean isStringTransformer;
    
    public PackageTransformer(Rules rules) {
        this.rules = rules;
    }
    
    private String[] fixNames(String[] names) {
        if (names == null)
            return null;
        String[] fixed = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            fixed[i] = rules.fixName(names[i]);
        }
        return fixed;
    }

    private Object fixValue(Object value) {
        if (value instanceof String) {
            return rules.fixString(className, (String)value);
        } else {
            return value;
        }
    }
    
    public void visit(int access, String name, String superName, String[] interfaces, String sourceFile) {
        className = name.replace('/', '.');
        isStringTransformer = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals("com/tonicsystems/jarjar/StringTransformer"))
                isStringTransformer = true;
            break;
        }
        cv.visit(access, rules.fixName(name), rules.fixName(superName), fixNames(interfaces), sourceFile);
    }

    public void visitAttribute(Attribute attr) {
        cv.visitAttribute(rules.fixAttribute(attr));
    }

    public void visitField(int access, String name, String desc, Object value, Attribute attrs) {
        cv.visitField(access, name, rules.fixDesc(desc), fixValue(value), rules.fixAttribute(attrs));
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cv.visitInnerClass(rules.fixName(name), rules.fixName(outerName), innerName, access);
    }

    public CodeVisitor visitMethod(int access, String name, String desc, String[] exceptions, Attribute attrs) {
        CodeVisitor inner = cv.visitMethod(access, name, rules.fixMethodDesc(desc), fixNames(exceptions), rules.fixAttribute(attrs));
        if (isStringTransformer)
            return inner;
        return new CodeAdapter(inner) {
            public void visitTypeInsn(int opcode, String desc) {
                cv.visitTypeInsn(opcode, (desc.charAt(0) == '[') ? rules.fixDesc(desc) : rules.fixName(desc));
            }
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                cv.visitFieldInsn(opcode, rules.fixName(owner), name, rules.fixDesc(desc));
            }
            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                cv.visitMethodInsn(opcode, rules.fixName(owner), name, rules.fixMethodDesc(desc));
            }
            public void visitLdcInsn(Object cst) {
                cv.visitLdcInsn(fixValue(cst));
            }
            public void visitMultiANewArrayInsn(String desc, int dims) {
                cv.visitMultiANewArrayInsn(rules.fixDesc(desc), dims);
            }
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                cv.visitTryCatchBlock(start, end, handler, rules.fixName(type));
            }
            public void visitLocalVariable(String name, String desc, Label start, Label end, int index) {
                cv.visitLocalVariable(name, rules.fixDesc(desc), start, end, index);
            }
            public void visitAttribute(Attribute attr) {
                cv.visitAttribute(rules.fixAttribute(attr));
            }
        };
    }
}

package com.tonicsystems.jarjar;

// TODO: pass Rules?
public interface StringTransformer
{
    static final String GENERATED_NAME = "com.tonicsystems.jarjar.GeneratedStringTransformer";
    
    public static final String MANIFEST_ATTRIBUTE = "JarJarStringTransformer";
    
    String transform(String value, String className, StringTransformer def);
}

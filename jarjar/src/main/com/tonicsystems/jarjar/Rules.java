package com.tonicsystems.jarjar;

import java.util.List;
import org.objectweb.asm.Attribute;

public interface Rules
{
    String fixPath(String path);
    String fixDesc(String desc);
    String fixName(String name);
    String fixMethodDesc(String desc);
    String fixString(String className, String value);
    Attribute fixAttribute(Attribute attrs);
}

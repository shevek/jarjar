package baz;

import com.tonicsystems.jarjar.StringTransformer;

public class BazStringTransformer implements StringTransformer
{
    public String transform(String value, String className, StringTransformer def)
    {
        if (className.equals("baz.Baz"))
            return value;
        return def.transform(value, className, def);
    }
}

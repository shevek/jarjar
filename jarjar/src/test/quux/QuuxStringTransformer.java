package quux;

import com.tonicsystems.jarjar.StringTransformer;

public class QuuxStringTransformer implements StringTransformer
{
    public String transform(String value, String className, StringTransformer def)
    {
        if (className.equals("quux.Quux"))
            return value;
        return def.transform(value, className, def);
    }
}

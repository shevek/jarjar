package bar;

import com.tonicsystems.jarjar.StringTransformer;

public class BarStringTransformer implements StringTransformer
{
    public String transform(String value, String className, StringTransformer def)
    {
        if (className.equals("bar.Bar"))
            return value;
        return def.transform(value, className, def);
    }
}

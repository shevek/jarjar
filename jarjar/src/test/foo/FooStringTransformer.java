package foo;

import com.tonicsystems.jarjar.StringTransformer;

public class FooStringTransformer implements StringTransformer
{
    public String transform(String value, String className, StringTransformer def)
    {
        if (className.equals("foo.Foo"))
            return value;
        return def.transform(value, className, def);
    }
}

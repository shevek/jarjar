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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.objectweb.asm.*;

class IoUtils
{
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    private IoUtils() {
    }

    public static ClassReader readClass(InputStream in) throws IOException {
        try {
            return new ClassReader(in);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ClassFormatError(e.getMessage());
        }
    }

    public static String readIntoString(InputStream in) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = null;
        while ((line = r.readLine()) != null)
            sb.append(line).append(LINE_SEPARATOR);
        return sb.toString();
    }

    public static String escapeStringLiteral(String value) {
        StringBuffer sb = new StringBuffer();
        sb.append("\"");
        char[] chars = value.toCharArray();
        for (int i = 0, size = chars.length; i < size; i++) {
            char ch = chars[i];
            switch (ch) {
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\b': sb.append("\\b"); break;
            case '\f': sb.append("\\f"); break;
            case '\t': sb.append("\\t"); break;
            case '\"': sb.append("\\\""); break;
            case '\\': sb.append("\\\\"); break;
            default:
                sb.append(ch);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public static byte[] toByteArray(InputStream is, byte[] buf) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pipe(is, baos, buf);
        return baos.toByteArray();
    }

    public static void pipe(InputStream is, OutputStream out, byte[] buf) throws IOException {
        for (;;) {
            int amt = is.read(buf);
            if (amt < 0)
                break;
            out.write(buf, 0, amt);
        }
    }

    public static void runMain(Object main, String[] args, String defCommand) throws Exception {
        if (args.length > 0) {
            String command = args[0];
            Method[] methods = main.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals(command)) {
                    String[] remaining = new String[args.length - 1];
                    System.arraycopy(args, 1, remaining, 0, remaining.length);
                    try {
                        method.invoke(main, (Object[])bindParameters(method, remaining));
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof IllegalArgumentException) {
                            System.err.println("Syntax error: " + cause.getMessage());
                        } else if (cause instanceof Exception) {
                            throw (Exception) cause;
                        } else {
                            throw e;
                        }
                    }
                    return;
                }
            }
        }
        if (defCommand != null)
            runMain(main, new String[]{ defCommand }, null);
    }

    private static Object[] bindParameters(Method method, String[] args) {
        List parameters = new ArrayList();
        Class[] parameterTypes = method.getParameterTypes();
        for (int i = 0, len = parameterTypes.length; i < len; i++) {
            Class type = parameterTypes[i];
            int remaining = Math.max(0, args.length - i);
            if (type.equals(String[].class)) {
                String[] rest = new String[remaining];
                System.arraycopy(args, 1, rest, 0, remaining);
                parameters.add(rest);
            } else if (remaining > 0) {
                parameters.add(convertParameter(args[i], parameterTypes[i]));
            } else {
                parameters.add(null);
            }
        }
        return parameters.toArray();
    }

    private static Object convertParameter(String arg, Class type) {
        if (type.equals(String.class)) {
            return arg;
        } else if (type.equals(Integer.class)) {
            return Integer.valueOf(arg, 10);
        } else if (type.equals(File.class)) {
            return new File(arg);
        } else {
            throw new UnsupportedOperationException("Unknown type " + type);
        }
    }
}

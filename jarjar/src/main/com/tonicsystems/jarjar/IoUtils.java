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
}

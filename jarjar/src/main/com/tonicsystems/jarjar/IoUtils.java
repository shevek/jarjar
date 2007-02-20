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

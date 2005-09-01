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

package com.tonicsystems.jarjar.util;

import java.io.*;
import java.util.*;

public class ClassHeaderReader
{
    private int access;
    private String thisClass;
    private String superClass;
    private String[] interfaces;

    public int getAccess() {
        return access;
    }
    
    public String getClassName() {
        return thisClass;
    }

    public String getSuperName() {
        return superClass;
    }

    public String[] getInterfaces() {
        return interfaces;
    }
    
    public ClassHeaderReader(InputStream in) throws IOException {
        try {
            DataInputStream data = new DataInputStream(in);
            int magic = data.readInt();
            int minorVersion = data.readUnsignedShort();
            int majorVersion = data.readUnsignedShort();
            if (magic != 0xCAFEBABE)
                throw new IOException("Bad magic number");
            // TODO: check version
            
            int constant_pool_count = data.readUnsignedShort();

            Map items = new TreeMap();
            for (int i = 1; i < constant_pool_count; i++) {
                int tag = data.readUnsignedByte();
                switch (tag) {
                case 9:  // Fieldref
                case 10: // Methodref
                case 11: // InterfaceMethodref
                case 3:  // Integer
                case 4:  // Float
                case 12: // NameAndType
                    skipFully(data, 4);
                    break;
                case 5:  // Long
                case 6:  // Double
                    skipFully(data, 8);
                    i++;
                    break;
                case 1:  // Utf8
                    items.put(new Integer(i), data.readUTF());
                    break;
                case 7:  // Class
                    items.put(new Integer(i), new Integer(data.readUnsignedShort()));
                    break;
                case 8:  // String
                    skipFully(data, 2);
                    break;
                default:
                    throw new IllegalStateException("Unknown constant pool tag " + tag);
                }
            }

            access = data.readUnsignedShort();
            thisClass = readClass(data.readUnsignedShort(), items);
            superClass = readClass(data.readUnsignedShort(), items);

            int interfaces_count = data.readUnsignedShort();
            interfaces = new String[interfaces_count];
            for (int i = 0; i < interfaces_count; i++) {
                interfaces[i] = readClass(data.readUnsignedShort(), items);
            }
        } finally {
            in.close();
        }
    }

    private static String readClass(int index, Map items) {
        if (items.get(new Integer(index)) == null) {
            throw new IllegalArgumentException("cannot find index " + index + " in " + items);
        }
        return readString(((Integer)items.get(new Integer(index))).intValue(), items);
    }

    private static String readString(int index, Map items) {
        return (String)items.get(new Integer(index));
    }
    
    private static void skipFully(DataInput data, int n) throws IOException {
        while (n > 0) {
            int amt = data.skipBytes(n);
            if (amt == 0) {
                data.readByte();
                n--;
            } else {
                n -= amt;
            }
        }
    }
}

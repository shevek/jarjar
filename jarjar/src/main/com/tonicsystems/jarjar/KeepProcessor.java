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

import com.tonicsystems.jarjar.util.*;
import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.*;

// TODO: this can probably be refactored into JarClassVisitor, etc.
class KeepProcessor extends Remapper implements JarProcessor
{
    private final ClassVisitor cv = new RemappingClassAdapter(new EmptyVisitor(), this);
    private final Wildcard[] wildcards;
    private final List roots = new ArrayList();
    private final Map depend = new HashMap();
    
    public KeepProcessor(List patterns) {
        wildcards = PatternElement.createWildcards(patterns);
    }

    public boolean isEnabled() {
        return wildcards.length > 0;
    }

    public Set getExcludes() {
        Set closure = new HashSet();
        closureHelper(closure, roots);
        Set removable = new HashSet(depend.keySet());
        removable.removeAll(closure);
        return removable;
    }

    private void closureHelper(Set closure, Collection process)
    {
        if (process == null)
            return;
        for (Iterator it = process.iterator(); it.hasNext();) {
            String name = (String)it.next();
            if (closure.add(name))
                closureHelper(closure, (Collection)depend.get(name));
        }
    }

    private Set curSet;
    private byte[] buf = new byte[0x2000];

    public boolean process(EntryStruct struct) throws IOException {
        try {
            if (struct.name.endsWith(".class")) {
                String name = struct.name.substring(0, struct.name.length() - 6);
                for (int i = 0; i < wildcards.length; i++)
                    if (wildcards[i].matches(name))
                        roots.add(name);
                depend.put(name, curSet = new HashSet());
                NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream();
                IoUtils.pipe(struct.in, out, buf);
                new ClassReader(out.getInputStream()).accept(cv, 0);
                curSet.remove(name);
                struct.in = out.getInputStream();
            }
        } catch (Exception ignore) { }
        return true;
    }

    protected String map(String key) {
        if (key.startsWith("java/") || key.startsWith("javax/"))
            return null;
        curSet.add(key);
        return null;
    }

    public Object mapValue(Object value) {
        if (value instanceof String) {
            String s = (String)value;
            if (PackageRemapper.isArrayForName(s)) {
                mapDesc(s.replace('.', '/'));
            } else if (isForName(s)) {
                map(s.replace('.', '/'));
            }
            return value;
        } else {
            return super.mapValue(value);
        }
    }

    // TODO: use this for package remapping too?
    private static boolean isForName(String value) {
        if (value.equals(""))
            return false;
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c != '.' && !Character.isJavaIdentifierPart(c))
                return false;
        }
        return true;
    }
    
    private static class NoCopyByteArrayOutputStream extends ByteArrayOutputStream
    {
        public InputStream getInputStream() {
            return new ByteArrayInputStream(buf, 0, count);
        }
    }
}

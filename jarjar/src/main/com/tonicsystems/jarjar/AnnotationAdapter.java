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

import org.objectweb.asm.AnnotationVisitor;

class AnnotationAdapter implements AnnotationVisitor
{
    protected AnnotationVisitor av;
    
    public AnnotationAdapter(AnnotationVisitor av) {
        this.av = av;
    }

    public void visit(String name, Object value) {
        av.visit(name, value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        return av.visitAnnotation(name, desc);
    }

    public AnnotationVisitor visitArray(String name) {
        return av.visitArray(name);
    }

    public void visitEnd() {
        av.visitEnd();
    }

    public void visitEnum(String name, String desc, String value) {
        av.visitEnum(name, desc, value);
    }
}

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

import org.objectweb.asm.signature.*;

abstract class SignatureAdapter
implements SignatureVisitor
{
    protected SignatureWriter sw;
    
    protected SignatureAdapter(SignatureWriter sw)
    {
        this.sw = sw;
    }
    
    public void visitFormalTypeParameter(String name) {
        sw.visitFormalTypeParameter(name);
    }
    
    public SignatureVisitor visitClassBound() {
        sw.visitClassBound();
        return this;
    }
    
    public SignatureVisitor visitInterfaceBound() {
        sw.visitInterfaceBound();
        return this;
    }
        
    public void visitBaseType(char descriptor) {
        sw.visitBaseType(descriptor);
    }
    
    public void visitTypeVariable(String name) {
        sw.visitTypeVariable(name);
    }
    
    public SignatureVisitor visitArrayType() {
        sw.visitArrayType();
        return this;
    }
    
    public void visitClassType(String name) {
        sw.visitClassType(name);
    }
    
    public void visitInnerClassType(String name) {
        sw.visitInnerClassType(name);
    }
    
    public void visitTypeArgument() {
        sw.visitTypeArgument();
    }
    
    public SignatureVisitor visitTypeArgument(char wildcard) {
        sw.visitTypeArgument(wildcard);
        return this;
    }
    
    public void visitEnd() {
        sw.visitEnd();
    }
    
    public SignatureVisitor visitSuperclass() {
        sw.visitSuperclass();
        return this;
    }
    
    public SignatureVisitor visitInterface() {
        sw.visitInterface();
        return this;
    }
    
    public SignatureVisitor visitParameterType() {
        sw.visitParameterType();
        return this;
    }
    
    public SignatureVisitor visitReturnType() {
        sw.visitReturnType();
        return this;
    }
    
    public SignatureVisitor visitExceptionType() {
        sw.visitExceptionType();
        return this;
    }
}  

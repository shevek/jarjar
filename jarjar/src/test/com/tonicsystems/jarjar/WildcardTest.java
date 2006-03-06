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

import junit.framework.*;

public class WildcardTest
extends TestCase
{
    public void testWildcards() {
        identifier("net.sf.cglib.**", "foo.@1", "net.sf.cglib.Bar", "foo.Bar");
        identifier("net.sf.cglib.**", "foo.@1", "net.sf.cglib.", "foo.");
        identifier("net.sf.cglib.**", "foo.@1", "net.sf.cglib.!", null);
    }

    private void identifier(String pattern, String result, String value, String expect) {
        helper(pattern, result, value, expect, Wildcard.STYLE_IDENTIFIER);
    }

    private void descriptor(String pattern, String result, String value, String expect) {
        helper(pattern, result, value, expect, Wildcard.STYLE_DESC);
    }

    private void helper(String pattern, String result, String value, String expect, int style) {
        Wildcard wc = new Wildcard(pattern, result);
        // System.err.println(wc);
        assertEquals(expect, wc.replace(value, style));
    }
    
    public WildcardTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WildcardTest.class);
    }
}

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

class Pair
{
    private Object o1;
    private Object o2;
        
    public Pair(Object o1, Object o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    public Object getObject1() {
        return o1;
    }

    public Object getObject2() {
        return o2;
    }

    public int hashCode() {
        return o1.hashCode() ^ o2.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Pair))
            return false;
        Pair other = (Pair)o;
        return o1.equals(other.o1) && o2.equals(other.o2);
    }
}

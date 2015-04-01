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
package com.tonicsystems.jarjar.dependencies;

import com.tonicsystems.jarjar.dependencies.AbstractDepHandler;
import java.io.IOException;

public class TextDepHandler extends AbstractDepHandler {

    private final Appendable out;

    public TextDepHandler(Appendable out, Level level) {
        super(level);
        this.out = out;
    }

    @Override
    protected void handle(String from, String to) throws IOException {
        out.append(from + " -> " + to + "\n");
    }
}

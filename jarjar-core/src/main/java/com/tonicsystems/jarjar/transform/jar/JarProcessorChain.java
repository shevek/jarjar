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
package com.tonicsystems.jarjar.transform.jar;

import com.tonicsystems.jarjar.util.EntryStruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.Nonnull;

public class JarProcessorChain extends ArrayList<JarProcessor> implements JarProcessor {

    public JarProcessorChain(@Nonnull Iterable<? extends JarProcessor> processors) {
        for (JarProcessor processor : processors)
            add(processor);
    }

    public JarProcessorChain(@Nonnull JarProcessor... processors) {
        this(Arrays.asList(processors));
    }

    /**
     * @param struct
     * @return <code>true</code> if the entry has run the complete chain
     * @throws IOException
     */
    @Override
    public boolean process(EntryStruct struct) throws IOException {
        for (JarProcessor processor : this) {
            if (!processor.process(struct)) {
                return false;
            }
        }
        return true;
    }
}

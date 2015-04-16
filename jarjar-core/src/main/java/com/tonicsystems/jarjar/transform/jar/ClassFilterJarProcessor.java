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

import com.tonicsystems.jarjar.transform.config.PatternUtils;
import com.tonicsystems.jarjar.transform.config.ClassDelete;
import com.tonicsystems.jarjar.util.ClassNameUtils;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

public class ClassFilterJarProcessor extends AbstractFilterJarProcessor {

    private final List<ClassDelete> patterns;

    public ClassFilterJarProcessor(@Nonnull Iterable<? extends ClassDelete> patterns) {
        this.patterns = PatternUtils.toList(patterns);
    }

    public ClassFilterJarProcessor(@Nonnull ClassDelete... patterns) {
        this(Arrays.asList(patterns));
    }

    public void addZap(@Nonnull ClassDelete pattern) {
        patterns.add(pattern);
    }

    @Override
    protected boolean isFiltered(@Nonnull String name) {
        if (!ClassNameUtils.isClass(name))
            return false;
        name = name.substring(0, name.length() - 6);
        for (ClassDelete pattern : patterns) {
            if (pattern.matches(name))
                return true;
        }
        return false;
    }
}

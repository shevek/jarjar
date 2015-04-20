/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.gradle.plugin.jarjar;

import com.google.common.base.Throwables;
import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.JarTransformer;
import com.tonicsystems.jarjar.transform.config.ClassDelete;
import com.tonicsystems.jarjar.transform.config.ClassRename;
import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.util.ConfigureUtil;

/**
 *
 * @author shevek
 */
public class JarjarController extends GroovyObjectSupport {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private final Project project;

    public JarjarController(@Nonnull Project project) {
        this.project = project;
    }

    public class Repackage extends GroovyObjectSupport {

        private final DefaultJarProcessor processor = new DefaultJarProcessor();
        public String name = "jarjar-" + SEQ.getAndIncrement() + ".jar";
        private FileCollection inputs = project.files();

        public void from(@Nonnull FileCollection files) {
            inputs = inputs.plus(files);
        }

        public void classRename(@Nonnull String pattern, @Nonnull String replacement) {
            processor.addClassRename(new ClassRename(pattern, replacement));
        }

        public void classDelete(@Nonnull String pattern) {
            processor.addClassDelete(new ClassDelete(pattern));
        }
    }

    public FileCollection repackage(@Nonnull Closure c) {
        final Repackage repackage = new Repackage();
        ConfigureUtil.configure(c, repackage);

        return new LazilyInitializedFileCollection() {
            @Override
            public FileCollection createDelegate() {
                try {
                    final File outputFile = new File(project.getBuildDir(), "jarjar/" + repackage.name);
                    outputFile.getParentFile().mkdirs();
                    ClassPath inputFiles = new ClassPath(project.getProjectDir(), repackage.inputs.getFiles());

                    JarTransformer transformer = new JarTransformer(outputFile, repackage.processor);
                    transformer.transform(inputFiles);

                    return new AbstractFileCollection() {
                        @Override
                        public String getDisplayName() {
                            return "Output of jarjar repackage " + repackage.name;
                        }

                        @Override
                        public Set<File> getFiles() {
                            return Collections.singleton(outputFile);
                        }
                    };
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        };
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.gradle.plugin.jarjar;

import com.google.common.base.Throwables;
import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.JarTransformer;
import com.tonicsystems.jarjar.transform.config.ClassClosureRoot;
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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.util.ConfigureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object appears as 'jarjar' in the project extensions.
 *
 * @author shevek
 */
public class JarjarController extends GroovyObjectSupport {

    private static final Logger LOG = LoggerFactory.getLogger(JarjarController.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private final Project project;

    public JarjarController(@Nonnull Project project) {
        this.project = project;
    }

    /**
     * This object defines the DSL for {@link #repackage(groovy.lang.Closure)}.
     */
    public class Repackage extends GroovyObjectSupport {

        private final DefaultJarProcessor processor = new DefaultJarProcessor();
        private FileCollection inputs = project.files();

        public void from(@Nonnull FileCollection files) {
            inputs = inputs.plus(files);
        }

        public void from(@Nonnull String dependency, Closure c) {
            Dependency d = project.getDependencies().create(dependency, c);
            Configuration configuration = project.getConfigurations().detachedConfiguration(d);
            inputs = inputs.plus(configuration);
        }

        public void from(@Nonnull String dependency) {
            from(dependency, Closure.IDENTITY);
        }

        public void classRename(@Nonnull String pattern, @Nonnull String replacement) {
            processor.addClassRename(new ClassRename(pattern, replacement));
        }

        public void classDelete(@Nonnull String pattern) {
            processor.addClassDelete(new ClassDelete(pattern));
        }

        public void classClosureRoot(@Nonnull String pattern) {
            processor.addClassClosureRoot(new ClassClosureRoot(pattern));
        }
    }

    public FileCollection repackage(@Nonnull Closure c) {
        final Repackage repackage = new Repackage();
        ConfigureUtil.configure(c, repackage);

        return new LazilyInitializedFileCollection() {
            @Override
            public FileCollection createDelegate() {
                try {
                    Set<File> inputFiles = repackage.inputs.getFiles();
                    String name;
                    NAME:
                    {
                        if (inputFiles.isEmpty()) {
                            name = "jarjar-" + SEQ.getAndIncrement() + ".jar";
                            break NAME;
                        }
                        name = inputFiles.iterator().next().getName();
                        if (name.endsWith(".jar"))
                            name = name.substring(0, name.length() - 4);
                        name = name + "-jarjar.jar";
                    }

                    final File outputFile = new File(project.getBuildDir(), "jarjar/" + name);
                    outputFile.getParentFile().mkdirs();
                    LOG.info("Running jarjar for {}", outputFile);

                    JarTransformer transformer = new JarTransformer(outputFile, repackage.processor);
                    transformer.transform(new ClassPath(project.getProjectDir(), inputFiles));

                    final String displayName = "JarjarOutput(" + name + ")";
                    return new AbstractFileCollection() {
                        @Override
                        public String getDisplayName() {
                            return displayName;
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

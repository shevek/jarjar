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
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.api.tasks.TaskOutputs;
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

        /**
         * Processes a FileCollection, which may be simple, a {@link Configuration},
         * or derived from a {@link TaskOutputs}.
         *
         * @param files The input FileCollection to consume.
         */
        public void from(@Nonnull FileCollection files) {
            inputs = inputs.plus(files);
        }

        /**
         * Processes a Dependency directly, which may be derived from
         * {@link DependencyHandler#create(java.lang.Object)},
         * {@link DependencyHandler#project(java.util.Map)},
         * {@link DependencyHandler#module(java.lang.Object)},
         * {@link DependencyHandler#gradleApi()}, etc.
         *
         * @param dependency The dependency to process.
         */
        public void from(@Nonnull Dependency dependency) {
            Configuration configuration = project.getConfigurations().detachedConfiguration(dependency);
            inputs = inputs.plus(configuration);
        }

        /**
         * Processes a dependency specified by name.
         *
         * @param dependencyNotation The dependency, in a notation described in {@link DependencyHandler}.
         * @param configClosure The closure to use to configure the dependency.
         * @see DependencyHandler
         */
        public void from(@Nonnull String dependencyNotation, Closure configClosure) {
            from(project.getDependencies().create(dependencyNotation, configClosure));
        }

        /**
         * Processes a dependency specified by name.
         *
         * @param dependencyNotation The dependency, in a notation described in {@link DependencyHandler}.
         */
        public void from(@Nonnull String dependencyNotation) {
            from(project.getDependencies().create(dependencyNotation));
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

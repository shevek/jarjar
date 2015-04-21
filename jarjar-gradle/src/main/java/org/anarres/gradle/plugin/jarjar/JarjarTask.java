/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.gradle.plugin.jarjar;

import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.JarTransformer;
import com.tonicsystems.jarjar.transform.config.ClassClosureRoot;
import com.tonicsystems.jarjar.transform.config.ClassDelete;
import com.tonicsystems.jarjar.transform.config.ClassRename;
import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor;
import groovy.lang.Closure;
import java.io.File;
import javax.annotation.Nonnull;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class JarjarTask extends ConventionTask {

    private static final Logger LOG = LoggerFactory.getLogger(JarjarTask.class);
    private File destinationDir;
    private String destinationName;

    private final DefaultJarProcessor processor = new DefaultJarProcessor();

    /**
     * Returns the directory where the archive is generated into.
     *
     * @return the directory
     */
    public File getDestinationDir() {
        File out = destinationDir;
        if (out == null)
            out = new File(getProject().getBuildDir(), "jarjar");
        return out;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    /**
     * Returns the file name of the generated archive.
     *
     * @return the name
     */
    public String getDestinationName() {
        String out = destinationName;
        if (out == null)
            out = getName() + ".jar";
        return out;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
     * The path where the archive is constructed.
     * The path is simply the {@code destinationDir} plus the {@code destinationName}.
     *
     * @return a File object with the path to the archive
     */
    @OutputFile
    public File getDestinationPath() {
        return new File(getDestinationDir(), getDestinationName());
    }

    /**
     * Processes a FileCollection, which may be simple, a {@link Configuration},
     * or derived from a {@link TaskOutputs}.
     *
     * @param files The input FileCollection to consume.
     */
    public void from(@Nonnull FileCollection files) {
        getInputs().files(files);
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
        Configuration configuration = getProject().getConfigurations().detachedConfiguration(dependency);
        from(configuration);
    }

    /**
     * Processes a dependency specified by name.
     *
     * @param dependencyNotation The dependency, in a notation described in {@link DependencyHandler}.
     * @param configClosure The closure to use to configure the dependency.
     * @see DependencyHandler
     */
    public void from(@Nonnull String dependencyNotation, Closure configClosure) {
        from(getProject().getDependencies().create(dependencyNotation, configClosure));
    }

    /**
     * Processes a dependency specified by name.
     *
     * @param dependencyNotation The dependency, in a notation described in {@link DependencyHandler}.
     */
    public void from(@Nonnull String dependencyNotation) {
        from(getProject().getDependencies().create(dependencyNotation));
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

    @TaskAction
    public void run() throws Exception {
        FileCollection inputFiles = getInputs().getFiles();
        final File outputFile = getDestinationPath();
        outputFile.getParentFile().mkdirs();
        LOG.info("Running jarjar for {}", outputFile);

        JarTransformer transformer = new JarTransformer(outputFile, processor);
        transformer.transform(new ClassPath(getProject().getProjectDir(), inputFiles));
    }
}

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

import com.tonicsystems.jarjar.transform.config.RulesFileParser;
import com.tonicsystems.jarjar.dependencies.TextDepHandler;
import com.tonicsystems.jarjar.dependencies.DepFind;
import com.tonicsystems.jarjar.dependencies.DepHandler;
import com.tonicsystems.jarjar.strings.StringDumper;
import com.tonicsystems.jarjar.transform.config.PatternElement;
import com.tonicsystems.jarjar.transform.StandaloneJarProcessor;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Main {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static enum Mode {

        strings, find, process;
    }

    private final OptionParser parser = new OptionParser();
    private final OptionSpec<Void> helpOption = parser.accepts("help")
            .forHelp();
    private final OptionSpec<Mode> modeOption = parser.accepts("mode")
            .withRequiredArg().ofType(Mode.class).defaultsTo(Mode.process).describedAs("Mode to run (strings, find, process)");
    private final OptionSpec<DepHandler.Level> levelOption = parser.accepts("level")
            .withRequiredArg().ofType(DepHandler.Level.class).defaultsTo(DepHandler.Level.CLASS).describedAs("Level for DepHandler.");
    private final OptionSpec<File> fromFilesOption = parser.accepts("from")
            .withRequiredArg().ofType(File.class).withValuesSeparatedBy(PATH_SEPARATOR).describedAs("Classpath for strings, find.");
    private final OptionSpec<File> rulesOption = parser.accepts("rules")
            .withRequiredArg().ofType(File.class).describedAs("Rules file.");
    private final OptionSpec<File> outputOption = parser.accepts("output")
            .withRequiredArg().ofType(File.class).describedAs("Output JAR file.");
    private final OptionSpec<File> filesOption = parser.nonOptions()
            .ofType(File.class).describedAs("JAR files or directories to process.");

    public void run(@Nonnull String[] args) throws Exception {
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.err);
            System.exit(1);
        }

        Mode mode = options.valueOf(modeOption);
        switch (mode) {
            case find:
                find(options);
                break;
            case process:
                process(options);
                break;
            case strings:
                strings(options);
                break;
            default:
                throw new IllegalArgumentException("Illegal mode " + mode);
        }
    }

    private static boolean isEmpty(@CheckForNull List<?> values) {
        if (values == null)
            return true;
        return values.isEmpty();
    }

    @Nonnull
    private <T> T valueOf(@Nonnull OptionSet options, @Nonnull OptionSpec<T> option) {
        T value = options.valueOf(option);
        if (value == null)
            throw new IllegalArgumentException(option + " is required.");
        return value;
    }

    @Nonnull
    private <T> List<T> valuesOf(@Nonnull OptionSet options, @Nonnull OptionSpec<T> option) {
        List<T> values = options.valuesOf(option);
        if (isEmpty(values))
            throw new IllegalArgumentException(option + " is required.");
        return values;
    }

    public void strings(@Nonnull OptionSet options) throws IOException {
        List<File> files = options.valuesOf(filesOption);
        File parent = new File(System.getProperty("user.dir"));
        new StringDumper().run(parent, files, System.out);
        System.out.flush();
    }

    public void find(@Nonnull OptionSet options) throws IOException {
        List<File> toFiles = valuesOf(options, filesOption);
        List<File> fromFiles = options.valuesOf(fromFilesOption);
        if (isEmpty(fromFiles))
            fromFiles = toFiles;
        DepHandler.Level level = valueOf(options, levelOption);
        DepHandler handler = new TextDepHandler(System.out, level);
        new DepFind().run(handler, fromFiles, toFiles);
        System.out.flush();
    }

    public void process(@Nonnull OptionSet options) throws IOException {
        File outputFile = valueOf(options, outputOption);
        File rulesFile = valueOf(options, rulesOption);
        List<File> files = valuesOf(options, filesOption);
        List<PatternElement> rules = RulesFileParser.parse(rulesFile);
        boolean skipManifest = Boolean.getBoolean("skipManifest");
        MainProcessor proc = new MainProcessor(rules, skipManifest);
        StandaloneJarProcessor.run(inJar, outJar, proc);
        proc.strip(outputFile);
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args);
        // MainUtil.runMain(new Main(), args, "help");
    }
}

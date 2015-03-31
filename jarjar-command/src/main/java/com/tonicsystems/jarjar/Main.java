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

import com.tonicsystems.jarjar.strings.StringDumper;
import com.tonicsystems.jarjar.config.PatternElement;
import com.tonicsystems.jarjar.util.StandaloneJarProcessor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Main {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

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
    private final OptionSpec<File> classPathOption = parser.acceptsAll(Arrays.asList("classpath", "cp"))
            .withRequiredArg().ofType(File.class).withValuesSeparatedBy(System.getProperty("path.separator")).describedAs("Classpath for strings, find.");
    private final OptionSpec<File> filesOption = parser.nonOptions()
            .ofType(File.class).describedAs("JAR files or directories to process.");

    public void run(@Nonnull String[] args) throws Exception {
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.err);
            System.exit(1);
        }

        switch (options.valueOf(modeOption)) {
            case find:
            case process:
            case strings:
                strings(options);
                break;
        }
    }

    public void strings(OptionSet options) throws IOException {
        List<File> files = options.valuesOf(classPathOption);
        File parent = new File(System.getProperty("user.dir"));
        new StringDumper().run(parent, files, System.out);
    }

    public void find(OptionSet options) throws IOException {
        DepHandler.Level level = options.valueOf(levelOption);
        if (level == null)
            throw new IllegalArgumentException("level and cp1 are required");
        if (cp2 == null) {
            cp2 = cp1;
        }
        DepHandler.Level levelFlag;
        if ("class".equals(level)) {
            levelFlag = DepHandler.Level.CLASS;
        } else if ("jar".equals(level)) {
            levelFlag = DepHandler.Level.JAR;
        } else {
            throw new IllegalArgumentException("unknown level " + level);
        }
        PrintWriter w = new PrintWriter(System.out);
        DepHandler handler = new TextDepHandler(w, levelFlag);
        new DepFind().run(cp1, cp2, handler);
        w.flush();
    }

    public void process(File rulesFile, File inJar, File outJar) throws IOException {
        if (rulesFile == null || inJar == null || outJar == null) {
            throw new IllegalArgumentException("rulesFile, inJar, and outJar are required");
        }
        List<PatternElement> rules = RulesFileParser.parse(rulesFile);
        boolean skipManifest = Boolean.getBoolean("skipManifest");
        MainProcessor proc = new MainProcessor(rules, skipManifest);
        StandaloneJarProcessor.run(inJar, outJar, proc);
        proc.strip(outJar);
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args);
        // MainUtil.runMain(new Main(), args, "help");
    }
}

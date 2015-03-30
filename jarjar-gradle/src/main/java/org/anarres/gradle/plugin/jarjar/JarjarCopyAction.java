/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.gradle.plugin.jarjar;

import java.io.File;
import javax.annotation.Nonnull;
import org.apache.tools.zip.UnixStat;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.file.copy.ZipCompressor;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.bundling.internal.Zip64RequiredException;
import org.gradle.internal.IoActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on ZipCopyAction from Gradle sources.
 *
 * @author shevek
 */
public class JarjarCopyAction implements CopyAction {

    private static final Logger LOG = LoggerFactory.getLogger(JarjarCopyAction.class);
    private final File zipFile;
    private final ZipCompressor compressor;
    private final DocumentationRegistry documentationRegistry;

    public JarjarCopyAction(File zipFile, ZipCompressor compressor, DocumentationRegistry documentationRegistry) {
        this.zipFile = zipFile;
        this.compressor = compressor;
        this.documentationRegistry = documentationRegistry;
    }

    @Nonnull
    @Override
    public WorkResult execute(@Nonnull final CopyActionProcessingStream stream) {
        final ZipOutputStream zipOutStr;

        try {
            zipOutStr = compressor.createArchiveOutputStream(zipFile);
        } catch (Exception e) {
            throw new GradleException(String.format("Could not create ZIP '%s'.", zipFile), e);
        }

        try {
            IoActions.withResource(zipOutStr, new Action<ZipOutputStream>() {
                @Override
                public void execute(@Nonnull ZipOutputStream outputStream) {
                    stream.process(new StreamAction(outputStream));
                }
            });
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof Zip64RequiredException) {
                throw new org.gradle.api.tasks.bundling.internal.Zip64RequiredException(
                        String.format("%s\n\nTo build this archive, please enable the zip64 extension.\nSee: %s", e.getCause().getMessage(), documentationRegistry.getDslRefForProperty(Zip.class, "zip64"))
                );
            }
        }

        return new SimpleWorkResult(true);
    }

    private class StreamAction implements CopyActionProcessingStreamAction {

        private final ZipOutputStream zipOutStr;

        public StreamAction(@Nonnull ZipOutputStream zipOutStr) {
            this.zipOutStr = zipOutStr;
        }

        @Override
        public void processFile(@Nonnull FileCopyDetailsInternal details) {
            if (details.isDirectory()) {
                visitDir(details);
            } else {
                visitFile(details);
            }
        }

        private void visitFile(@Nonnull FileCopyDetails fileDetails) {
            try {
                ZipEntry archiveEntry = new ZipEntry(fileDetails.getRelativePath().getPathString());
                archiveEntry.setTime(fileDetails.getLastModified());
                archiveEntry.setUnixMode(UnixStat.FILE_FLAG | fileDetails.getMode());
                zipOutStr.putNextEntry(archiveEntry);
                fileDetails.copyTo(zipOutStr);
                zipOutStr.closeEntry();
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
            }
        }

        private void visitDir(@Nonnull FileCopyDetails dirDetails) {
            try {
                // Trailing slash in name indicates that entry is a directory
                ZipEntry archiveEntry = new ZipEntry(dirDetails.getRelativePath().getPathString() + '/');
                archiveEntry.setTime(dirDetails.getLastModified());
                archiveEntry.setUnixMode(UnixStat.DIR_FLAG | dirDetails.getMode());
                zipOutStr.putNextEntry(archiveEntry);
                zipOutStr.closeEntry();
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to ZIP '%s'.", dirDetails, zipFile), e);
            }
        }
    }

}

/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beryx.jlink.util

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class JdkUtil {
    private static final Logger LOGGER = Logging.getLogger(JdkUtil.class)

    static class JdkDownloadOptions implements Serializable {
        transient final Project project
        String downloadDir
        String archiveName
        String archiveExtension
        String pathToHome
        boolean overwrite

        JdkDownloadOptions(Project project, String targetPlatform, String downloadUrl) {
            this.project = project
            this.downloadDir = "$project.buildDir/jdks/$targetPlatform"
            this.archiveName = "jdk"
            def urlPath = new URL(downloadUrl).path
            if(urlPath.endsWith(".tar.gz")) archiveExtension = "tar.gz"
            if(urlPath.endsWith(".zip")) archiveExtension = "zip"
        }

        void validate() {
            if(!project) throw new GradleException("Internal error: project not set in JdkDownloadOptions")
            if(!downloadDir) throw new GradleException("Please provide a value for 'downloadDir' when calling the 'jdkDownload' method")
            if(!archiveName) throw new GradleException("Please provide a value for 'archiveName' when calling the 'jdkDownload' method")
            if(!archiveExtension) throw new GradleException("Cannot infer the archive type. Please provide a value for 'archiveExtension' when calling the 'jdkDownload' method. Accepted values: 'tar.gz', 'zip'")
        }
    }

    static String downloadFrom(String url, JdkDownloadOptions options) {
        options.validate()
        def archiveFile = new File("${options.downloadDir}/${options.archiveName}.${options.archiveExtension}")
        def archiveDir = archiveFile.parentFile
        maybeCreateDir(archiveDir)
        if(options.overwrite) {
            archiveFile.delete()
            if(archiveFile.exists()) throw new GradleException("Cannot delete $archiveFile")
        } else {
            def pathToHome = options.pathToHome ?: detectPathToHome(archiveDir)
            if(pathToHome) {
                LOGGER.info("JDK found at $archiveDir/$pathToHome; skipping download from $url")
                return pathToHome
            }
        }
        if(!archiveFile.file) {
            LOGGER.info("Downloading from $url into $archiveFile")
            new URL(url).withInputStream{ is -> archiveFile.withOutputStream{ it << is }}
        }
        return unpackJdk(archiveDir, archiveFile, options)
    }

    private static String unpackJdk(File archiveDir, File archiveFile, JdkDownloadOptions options) {
        if(!archiveFile.file) throw new GradleException("Archive file $archiveFile does not exist.")
        LOGGER.info("Unpacking $archiveFile")
        options.project.copy { CopySpec spec ->
            spec.from ((options.archiveExtension == 'tar.gz')
                    ? options.project.tarTree(options.project.resources.gzip(archiveFile))
                    : options.project.zipTree(archiveFile))
            spec.into archiveDir
        }
        if(options.pathToHome) {
            if(!isJdkHome(new File("$archiveDir/$options.pathToHome"))) {
                LOGGER.warn("JDK home not found at $archiveDir/$options.pathToHome! Please check the the unpacked archive.")
            }
            return options.pathToHome
        } else {
            def pathToHome = detectPathToHome(archiveDir)
            if(!pathToHome) throw new GradleException("Cannot detect JDK home in $archiveDir. Check the unpacked archive and/or try setting `pathToHome` when calling the 'jdkDownload' method")
            return pathToHome
        }
    }

    private static String detectPathToHome(File dir) {
        def dirs = [dir]
        dir.eachDirRecurse { dirs << it }
        def homeDir = dirs.find { isJdkHome(it) }
        if(!homeDir) return null
        def relPath = dir.toPath().relativize(homeDir.toPath())
        return relPath ?: '.'
    }

    private static boolean isJdkHome(File dir) {
        new File("$dir/bin/java").file || new File("$dir/bin/java.exe").file
    }

    private static void maybeCreateDir(File dir) {
        if (!dir.directory) {
            if (dir.exists()) throw new GradleException("$dir exists but it's not a directory.")
            dir.mkdirs()
            if (!dir.directory) throw new GradleException("Cannot create directory $dir.")
        }
    }
}

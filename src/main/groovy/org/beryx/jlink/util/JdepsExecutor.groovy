/*
 * Copyright 2018 the original author or authors.
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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations

@CompileStatic
class JdepsExecutor {
    private static final Logger LOGGER = Logging.getLogger(JdepsExecutor.class);

    final FileSystemOperations fileSystemOperations
    final ExecOperations execOperations

    @TupleConstructor
    static class Result {
        final int exitValue
        final File moduleInfoFile
        final String output
    }

    JdepsExecutor(FileSystemOperations fileSystemOperations, ExecOperations execOperations) {
        this.fileSystemOperations = fileSystemOperations
        this.execOperations = execOperations
    }

    @CompileDynamic
    Result genModuleInfo(File jarFile, File targetDir, String jlinkJarsDirPath, String javaHome) {
        LOGGER.info("Generating module-info in ${targetDir}...")
        fileSystemOperations.delete { it.delete(targetDir) }
        targetDir.mkdirs()

        def outputStream = new ByteArrayOutputStream()

        def execResult = execOperations.exec { spec ->
            spec.ignoreExitValue = true
            spec.standardOutput = outputStream
            spec.commandLine = [
                    "$javaHome/bin/jdeps",
                    '-v',
                    '--generate-module-info',
                    targetDir.path,
                    '--module-path',
                    "$javaHome/jmods/$File.pathSeparatorChar$jlinkJarsDirPath",
                    jarFile.path
            ]
        }

        def files = targetDir.listFiles()
        def moduleInfoFile =  files?.length ? files[0] : null
        return new Result(execResult.exitValue, moduleInfoFile, outputStream.toString())
    }

}

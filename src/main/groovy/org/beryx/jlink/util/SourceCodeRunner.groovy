/*
 * Copyright 2021 the original author or authors.
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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.util.concurrent.TimeUnit

@CompileStatic
class SourceCodeRunner {
    private static final Logger LOGGER = Logging.getLogger(SourceCodeRunner.class);

    final String javaHome
    final String className
    final String sourceCode
    long timeoutSeconds = 120

    SourceCodeRunner(String javaHome, String className, String sourceCode) {
        this.javaHome = javaHome
        this.className = className
        this.sourceCode = sourceCode
    }

    String getOutput(String[] args) {
        def path = Files.createTempDirectory("badass-")
        def javaFile = path.resolve("${className}.java").toFile()
        javaFile << sourceCode

        def javacCmd = "$javaHome/bin/javac -cp . -d . ${className}.java"
        LOGGER.info("Executing: $javacCmd")
        def javacProc = javacCmd.execute(null as String[], path.toFile())
        def javacErrOutput = new StringBuilder()
        javacProc.consumeProcessErrorStream(javacErrOutput)
        if (!javacProc.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new GradleException("javac ${className}.java hasn't exited after $timeoutSeconds seconds.")
        }
        String javacOutput = javacProc.text
        LOGGER.info(javacOutput)
        if (javacProc.exitValue()) {
            throw new GradleException("javac ${className}.java failed: $javacErrOutput")
        }
        if (javacErrOutput.size() > 0) LOGGER.error("javac failed: $javacErrOutput")

        def cmdArray = ["$javaHome/bin/java", "-cp", ".", "${className}"]
        if(args) {
            cmdArray.addAll(Arrays.asList(args))
        }
        LOGGER.info("Executing: $cmdArray")
        def javaProc = Runtime.runtime.exec((cmdArray as String[]), null, path.toFile())

        def javaErrOutput = new StringBuilder()
        def javaOutput = new StringBuilder()
        javaProc.consumeProcessOutput(javaOutput, javaErrOutput)
        if (!javaProc.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new GradleException("java ${className} hasn't exited after $timeoutSeconds seconds.")
        }

        LOGGER.info(javaOutput.toString())
        if (javaProc.exitValue()) {
            throw new GradleException("java ${className} failed: $javaErrOutput")
        }
        if (javaErrOutput.size() > 0) LOGGER.error("java failed: $javaErrOutput")
        return javaOutput.toString()
    }
}

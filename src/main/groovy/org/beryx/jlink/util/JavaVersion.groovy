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

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.util.concurrent.TimeUnit

@CompileStatic
class JavaVersion {
    private static final Logger LOGGER = Logging.getLogger(JavaVersion.class);

    static final String sourceCode = '''
import java.lang.reflect.Method;

public class JavaVersion {
    public static void main(String[] args) {
        System.out.print(getVersion());
    }

    public static int getVersion() {
        try {
            Method mVersion = Runtime.class.getMethod("version");
            Object version = mVersion.invoke(null);
            try {
                Method mFeature = version.getClass().getMethod("feature");
                return (int)mFeature.invoke(version);
            } catch (Exception e) {
                Method mMajor = version.getClass().getMethod("major");
                return (int)mMajor.invoke(version);
            }
        } catch (Exception e) {
            try {
                String prop = System.getProperty("java.specification.version");
                int pos = prop.lastIndexOf('.');
                return Integer.parseInt(prop.substring(pos + 1));
            } catch (Exception e1) {
                return 8;
            }
        }
    }
}
'''

    static int get(String javaHome) {
        def path = Files.createTempDirectory("badass-")
        def javaFile = path.resolve('JavaVersion.java').toFile()
        javaFile << sourceCode

        def javacCmd = "$javaHome/bin/javac -cp . -d . JavaVersion.java"
        LOGGER.info("Executing: $javacCmd")
        def javacProc = javacCmd.execute([], path.toFile())
        def javacErrOutput = new StringBuilder()
        javacProc.consumeProcessErrorStream(javacErrOutput)
        if(!javacProc.waitFor(30, TimeUnit.SECONDS)) {
            throw new GradleException("javac JavaVersion.java hasn't exited after 30 seconds.")
        }
        String javacOutput = javacProc.text
        LOGGER.info(javacOutput)
        if(javacProc.exitValue()) {
            throw new GradleException("javac JavaVersion.java failed: $javacErrOutput")
        }
        if(javacErrOutput.size() > 0) LOGGER.error("javac failed: $javacErrOutput")

        def javaCmd = "$javaHome/bin/java -cp . JavaVersion"
        LOGGER.info("Executing: $javaCmd")
        def javaProc = javaCmd.execute([], path.toFile())
        def javaErrOutput = new StringBuilder()
        javaProc.consumeProcessErrorStream(javaErrOutput)
        if(!javaProc.waitFor(30, TimeUnit.SECONDS)) {
            throw new GradleException("java JavaVersion hasn't exited after 30 seconds.")
        }

        String javaOutput = javaProc.text
        LOGGER.info(javaOutput)
        if(javaProc.exitValue()) {
            throw new GradleException("java JavaVersion failed: $javaErrOutput")
        }
        if(javaErrOutput.size() > 0) LOGGER.error("java failed: $javaErrOutput")

        try {
            return javaOutput as int
        } catch (Exception e) {
            throw new GradleException("Cannot parse java version: $javaOutput")
        }
    }
}

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

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.nio.file.Files

class JavaVersion {
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

    static int get(Project project, String javaHome) {
        def infoLog = {s -> project ? project.logger.info(s) : println(s)}
        def errorLog = {s -> project ? project.logger.error(s) : println(s)}
        def path = Files.createTempDirectory("badass-")
        def javaFile = path.resolve('JavaVersion.java').toFile()
        javaFile << sourceCode

        def javacCmd = "$javaHome/bin/javac -cp . -d . JavaVersion.java"
        infoLog("Executing: $javacCmd")
        def javacProc = javacCmd.execute([], path.toFile())
        def javacErrOutput = new StringBuilder()
        javacProc.consumeProcessErrorStream(javacErrOutput)

        String javacOutput = javacProc.text
        infoLog(javacOutput)
        if(javacProc.exitValue()) {
            throw new GradleException("javac JavaVersion.java failed: $javacErrOutput")
        }
        if(javacErrOutput.size() > 0) errorLog("javac failed: $javacErrOutput")

        def javaCmd = "$javaHome/bin/java -cp . JavaVersion"
        infoLog("Executing: $javaCmd")
        def javaProc = javaCmd.execute([], path.toFile())
        def javaErrOutput = new StringBuilder()
        javaProc.consumeProcessErrorStream(javaErrOutput)

        String javaOutput = javaProc.text
        infoLog(javaOutput)
        if(javaProc.exitValue()) {
            throw new GradleException("java JavaVersion failed: $javaErrOutput")
        }
        if(javaErrOutput.size() > 0) errorLog("java failed: $javaErrOutput")

        try {
            return javaOutput as int
        } catch (Exception e) {
            throw new GradleException("Cannot parse java version: $javaOutput")
        }
    }
}

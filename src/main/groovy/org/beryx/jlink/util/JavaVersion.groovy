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

@CompileStatic
class JavaVersion {
    private static final Logger LOGGER = Logging.getLogger(JavaVersion.class)

    static int get(String javaHome) {
        def runner = new SourceCodeRunner(javaHome, 'JavaVersion', sourceCode)
        String javaOutput = runner.getOutput()
        LOGGER.info "javaVersion($javaHome): $javaOutput"
        try {
            return javaOutput as int
        } catch (Exception e) {
            throw new GradleException("Cannot parse java version: $javaOutput")
        }
    }

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
}

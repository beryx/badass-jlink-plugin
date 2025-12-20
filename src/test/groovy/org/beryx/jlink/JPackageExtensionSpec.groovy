/*
 * Copyright 2025 the original author or authors.
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
package org.beryx.jlink

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path

class JPackageExtensionSpec extends Specification {
    @TempDir Path testProjectDir

    def "should allow configuring jpackage as a top-level extension"() {
        given:
        def buildFile = testProjectDir.resolve('build.gradle').toFile()
        buildFile << """
            plugins {
                id 'java'
                id 'org.beryx.jlink'
            }
            jlink {
                launcher {
                    name = 'hello'
                }
            }
            jpackage {
                imageName = 'my-custom-image'
                vendor = 'my-vendor'
            }
        """.stripIndent()

        // Create a dummy source file to satisfy jlink
        def srcDir = testProjectDir.resolve('src/main/java/com/example')
        srcDir.toFile().mkdirs()
        srcDir.resolve('Main.java').toFile() << """
            package com.example;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
        """.stripIndent()
        
        testProjectDir.resolve('src/main/java/module-info.java').toFile() << """
            module com.example {
                exports com.example;
            }
        """.stripIndent()

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('help') // just to trigger configuration
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "should share data between jlink.jpackage and top-level jpackage"() {
        given:
        def buildFile = testProjectDir.resolve('build.gradle').toFile()
        buildFile << """
            plugins {
                id 'java'
                id 'org.beryx.jlink'
            }
            jlink {
                jpackage {
                    vendor = 'jlink-vendor'
                }
            }
            task checkJPackage {
                doLast {
                    assert jpackage.vendor == 'jlink-vendor'
                    jpackage.vendor = 'top-vendor'
                    assert jlink.jpackageData.get().vendor == 'top-vendor'
                }
            }
        """.stripIndent()

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('checkJPackage')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "should use jlink.javaHome as default for jpackageHome"() {
        given:
        def buildFile = testProjectDir.resolve('build.gradle').toFile()
        def dummyJdk = testProjectDir.resolve('dummy-jdk').toFile()
        dummyJdk.mkdirs()
        
        buildFile << """
            plugins {
                id 'java'
                id 'org.beryx.jlink'
            }
            jlink {
                javaHome = file('${dummyJdk.absolutePath.replace('\\', '/')}')
            }
            task checkJPackageHome {
                doLast {
                    println "[DEBUG_LOG] JPackageHome: \${jpackage.getJPackageHomeOrDefault()}"
                    assert jpackage.getJPackageHomeOrDefault() == file('${dummyJdk.absolutePath.replace('\\', '/')}').absolutePath
                }
            }
        """.stripIndent()

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('checkJPackageHome')
                .build()

        then:
        result.output.contains("[DEBUG_LOG] JPackageHome: ${dummyJdk.absolutePath}")
    }
}

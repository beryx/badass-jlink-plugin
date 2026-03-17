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
package org.beryx.jlink

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

class GradleCompatibilitySpec extends AbstractJlinkPluginTest {
    @Unroll
    def "should execute task with Gradle #gradleVersion, moduleName=#moduleName, launcherName=#launcherName, mainClass=#mainClass and mergedModuleName=#mergedModuleName"() {
        when:
        setUpHelloLogbackBuild(moduleName, launcherName, mainClass, mergedModuleName)
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, expectedLauncherName, 'LOG: Hello, modular Java!')

        where:
        moduleName              | gradleVersion | launcherName | mainClass                   | mergedModuleName                    | expectedLauncherName
        null                    | '7.6'         | null         | null                        | null                                | 'modular-hello'
        'modular.example.hello' | '7.4'         | 'run-hello'  | ''                          | 'org.example.my.test.merged.module' | 'run-hello'
        null                    | '7.4'         | null         | 'org.example.modular.Hello' | null                                | 'modular-hello'
        'modular.example.hello' | '7.6'         | null         | null                        | null                                | 'modular-hello'
        'modular.example.hello' | '7.4'         | 'run-hello'  | 'org.example.modular.Hello' | null                                | 'run-hello'
        'modular.example.hello' | '7.6'         | 'run-hello'  | null                        | null                                | 'run-hello'
        null                    | '8.12.1'      | null         | null                        | null                                | 'modular-hello'
        'modular.example.hello' | '8.12.1'      | 'run-hello'  | ''                          | 'org.example.my.test.merged.module' | 'run-hello'
        null                    | '8.12.1'      | null         | 'org.example.modular.Hello' | null                                | 'modular-hello'
        'modular.example.hello' | '8.12.1'      | null         | null                        | null                                | 'modular-hello'
        'modular.example.hello' | '8.12.1'      | 'run-hello'  | 'org.example.modular.Hello' | null                                | 'run-hello'
        'modular.example.hello' | '8.12.1'      | 'run-hello'  | null                        | null                                | 'run-hello'
        'modular.example.hello' | '9.0.0'       | 'run-hello'  | null                        | null                                | 'run-hello'
    }

    @Unroll
    def "should create runtime image of project #projectDir with Gradle #gradleVersion"() {
        when:
        setUpBuild(projectDir)
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK_ZIP, "-is")
                .build();
        def imageBinDir = new File(testProjectDir.toFile(), "build/$imageDir/bin")
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "$expectedLauncherName$launcherExt")
        def imageZipFile = new File(testProjectDir.toFile(), "build/$imageZip")

        then:
        result.task(":$JlinkPlugin.TASK_NAME_JLINK").outcome == TaskOutcome.SUCCESS
        imageLauncher.exists()
        imageLauncher.canExecute()
        imageZipFile.exists()

        where:
        projectDir                  | gradleVersion | imageDir        | imageZip            | expectedLauncherName
        'hello-javafx'              | '7.4'         | 'helloFX'       | 'helloFX.zip'       | 'helloFX'
        'hello-javafx-log4j-2.11.1' | '7.6'         | 'image'         | 'image.zip'         | 'helloFX'
        'hello-javafx-jep493'       | '9.1.0'       | 'helloFXJep493' | 'helloFXJep493.zip' | 'helloFXJep493'
    }
}

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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

class JlinkPluginSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    def cleanup() {
        println "CLEANUP"
    }

    def setUpBuild(String projectDir) {
        new AntBuilder().copy(todir: testProjectDir.root) {
            fileset(dir: "src/test/resources/$projectDir")
        }
        new File(testProjectDir.root, "build.gradle")
    }

    def setUpHelloLogbackBuild(String moduleName, String launcherName, String mainClass, String mergedModuleName) {
        File buildFile = setUpBuild('hello-logback')
        buildFile << '''
            jlink {
                options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']                
        '''.stripIndent()
        if(moduleName) buildFile << "    moduleName = '$moduleName'\n"
        if(launcherName) buildFile << "    launcher {name = '$launcherName'}\n"
        if(mainClass) buildFile << "    mainClass = '$mainClass'\n"
        if(mergedModuleName) buildFile << "    mergedModuleName = '$mergedModuleName'\n"
        buildFile <<
        ''' |    mergedModule {
            |        requires 'java.naming';
            |        requires 'java.xml';
            |    }
        |'''.stripMargin()
        buildFile << '}\n'
        println "Executing build script:\n${buildFile.text}"
    }

    @Unroll
    def "should execute task with Gradle #gradleVersion, moduleName=#moduleName, launcherName=#launcherName, mainClass=#mainClass and mergedModuleName=#mergedModuleName"() {
        when:
        setUpHelloLogbackBuild(moduleName, launcherName, mainClass, mergedModuleName)
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();
        def imageBinDir = new File(testProjectDir.root, 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "$expectedLauncherName$launcherExt")

        then:
        result.task(":$JlinkPlugin.TASK_NAME_JLINK").outcome == TaskOutcome.SUCCESS
        imageLauncher.exists()
        imageLauncher.canExecute()

        when:
        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, out)
        def outputText = out.toString()

        then:
        outputText.trim() == 'LOG: Hello, modular Java!'

        where:
        moduleName              | gradleVersion | launcherName | mainClass                   | mergedModuleName                    | expectedLauncherName
        null                    | '4.8'         | null         | null                        | null                                | 'modular-hello'
        'modular.example.hello' | '4.10.3'      | 'run-hello'  | ''                          | 'org.example.my.test.merged.module' | 'run-hello'
        null                    | '5.0'         | null         | 'org.example.modular.Hello' | null                                | 'modular-hello'
        'modular.example.hello' | '5.1.1'       | null         | null                        | null                                | 'modular-hello'
    }

    @Unroll
    def "should create runtime image of project #projectDir with Gradle #gradleVersion"() {
        when:
        setUpBuild(projectDir)
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK_ZIP, "-is")
                .build();
        def imageBinDir = new File(testProjectDir.root, "build/$imageDir/bin")
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "$expectedLauncherName$launcherExt")
        def imageZipFile = new File(testProjectDir.root, "build/$imageZip")

        then:
        result.task(":$JlinkPlugin.TASK_NAME_JLINK").outcome == TaskOutcome.SUCCESS
        imageLauncher.exists()
        imageLauncher.canExecute()
        imageZipFile.exists()

        where:
        projectDir                  | gradleVersion | imageDir  | imageZip      | expectedLauncherName
        'hello-javafx'              | '4.8'         | 'helloFX' | 'helloFX.zip' | 'helloFX'
        'hello-javafx-log4j-2.11.1' | '5.0'         | 'image'   | 'image.zip'   | 'helloFX'
    }

}

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
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

import java.nio.file.Path

class JlinkPluginSpec extends Specification {
    @TempDir Path testProjectDir

    def cleanup() {
        println "CLEANUP"
    }

    def setUpBuild(String projectDir, String buildScriptName = 'build.gradle') {
        new AntBuilder().copy(todir: testProjectDir) {
            def options = [dir: "src/test/resources/$projectDir"]
            if(buildScriptName != 'build.gradle') {
                options.excludes = 'build.gradle'
            }
            fileset(options)
        }
        if(buildScriptName != 'build.gradle') {
            new File(testProjectDir.toFile(), buildScriptName).renameTo("$testProjectDir/build.gradle")
        }
        new File(testProjectDir.toFile(), 'build.gradle')
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

    def "should be compatible with the new JPMS features introduced in Gradle 6.4"() {
        when:
        setUpBuild('hello-logback', 'build.modular.gradle')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('7.6')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'modular-hello', 'LOG: Hello, modular Java!')
    }

    @Ignore("Gradle bug: https://github.com/gradle/native-platform/issues/274")
    def "should use configured toolchain"() {
        when:
        setUpBuild('hello-toolchain')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('7.6')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'hello-toolchain', 'LOG: line from: (30,80) to: (20,50)')
    }

    @Unroll
    def "should execute task with Gradle #gradleVersion, moduleName=#moduleName, launcherName=#launcherName, mainClass=#mainClass and mergedModuleName=#mergedModuleName"() {
        when:
        setUpHelloLogbackBuild(moduleName, launcherName, mainClass, mergedModuleName)
        BuildResult result = GradleRunner.create()
                .withDebug(true)
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
    }

    @Unroll
    def "should create runtime image of project #projectDir with Gradle #gradleVersion"() {
        when:
        setUpBuild(projectDir)
        BuildResult result = GradleRunner.create()
                .withDebug(true)
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
        projectDir                  | gradleVersion | imageDir  | imageZip      | expectedLauncherName
        'hello-javafx'              | '7.4'         | 'helloFX' | 'helloFX.zip' | 'helloFX'
        'hello-javafx-log4j-2.11.1' | '7.6'         | 'image'   | 'image.zip'   | 'helloFX'
    }

    def "should adjust qualified opens in module-info"() {
        when:
        File buildFile = setUpBuild('opens-to-jaxb')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('7.6')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'xmlprint',
            '''
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <product>
                <id>100</id>
                <name>pizza</name>
                <price>3.25</price>
            </product>         
            '''.stripIndent().strip())
    }

    def "should create image of project with local dependencies"() {
        when:

        File buildFile = setUpBuild('local-deps')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withGradleVersion('7.6')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'reverseHello', '!dlrow ,olleH')
    }

    def "should create runtime image of project with BOM"() {
        when:
        File buildFile = setUpBuild('hello-bom')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withGradleVersion('7.6')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'helloBom', '{"from":"Alice","to":"Bob","greeting":"Hello"}')
    }

    def "should create image of project with multiple launchers"() {
        when:

        File buildFile = setUpBuild('multi-launch')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withGradleVersion('7.6')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'hello', 'Hello, world!')
        checkOutput(result, 'helloAgain', 'Hello again!')
        checkOutput(result, 'howdy', 'Howdy!')
    }

    def "should create image of project with multiple launchers using kotlin DSL"() {
        when:

        File buildFile = setUpBuild('multi-launch-kotlin-dsl')
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withGradleVersion('7.6')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'hello', 'Hello, world!')
        checkOutput(result, 'helloAgain', 'Hello again!')
        checkOutput(result, 'howdy', 'Howdy!')
    }

    private boolean checkOutput(BuildResult result, String imageName, String expectedOutput) {
        def imageBinDir = new File(testProjectDir.toFile(), 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''

        assert result.task(":$JlinkPlugin.TASK_NAME_JLINK").outcome == TaskOutcome.SUCCESS

        def imageLauncher = new File(imageBinDir, "$imageName$launcherExt")
        assert imageLauncher.exists()
        assert imageLauncher.canExecute()

        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        def err = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, err)
        def outputText = out.toString()
        assert outputText.trim() == expectedOutput

        true
    }
}

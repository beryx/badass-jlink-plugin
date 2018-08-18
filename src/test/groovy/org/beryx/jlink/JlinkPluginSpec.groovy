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

    def setUpBuild(boolean withBeforeZip, String moduleName, String launcherName, String mainClass, String mergedModuleName) {
        new AntBuilder().copy( todir: testProjectDir.root ) {
            fileset( dir: 'src/test/resources/hello' )
        }

        File buildFile = new File(testProjectDir.root, "build.gradle")
        if(!withBeforeZip) {
            buildFile << 'jlink {\n'
        } else {
            buildFile << '''
            jlink {
                options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
                beforeZip {
                    copy {
                        from('src/main/resources')
                        into("$buildDir/image/bin")
                    }
                }
        '''.stripIndent()
        }
        if(moduleName) buildFile << "    moduleName = '$moduleName'\n"
        if(launcherName) buildFile << "    launcherName = '$launcherName'\n"
        if(mainClass) buildFile << "    mainClass = '$mainClass'\n"
        if(mergedModuleName) buildFile << "    mergedModuleName = '$mergedModuleName'\n"
        buildFile <<
        ''' |    mergedModule {
            |        requires 'java.naming';
            |        requires 'java.xml';
            |        requires 'java.xml.bind';
            |    }
        |'''.stripMargin()
        buildFile << '}\n'
        println "Executing build script:\n${buildFile.text}"
    }

    @Unroll
    def "should execute task with useBeforeZip=#useBeforeZip, moduleName=#moduleName, launcherName=#launcherName, mainClass=#mainClass and mergedModuleName=#mergedModuleName"() {
        when:
        setUpBuild(useBeforeZip, moduleName, launcherName, mainClass, mergedModuleName)
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME, "-is")
                .build();
        def imageBinDir = new File(testProjectDir.root, 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "$expectedLauncherName$launcherExt")
        def logbackXml = new File(imageBinDir, "logback.xml")

        then:
        result.task(":$JlinkPlugin.TASK_NAME").outcome == TaskOutcome.SUCCESS
        imageLauncher.exists()
        useBeforeZip == logbackXml.exists()

        when:
        imageLauncher.setExecutable(true)
        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, out)
        def outputText = out.toString()

        then:
        outputText.trim() == 'LOG: Hello, modular Java!'

        where:
        useBeforeZip | moduleName              | launcherName | mainClass                   | mergedModuleName                    | expectedLauncherName
        false        | null                    | null         | null                        | null                                | 'modular-hello'
        true         | 'modular.example.hello' | 'run-hello'  | ''                          | 'org.example.my.test.merged.module' | 'run-hello'
        true         | null                    | null         | 'org.example.modular.Hello' | null                                | 'modular-hello'
    }

}

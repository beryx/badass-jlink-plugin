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
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.OperatingSystem

import java.nio.file.Path

abstract class AbstractJlinkPluginTest extends Specification {
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

    protected boolean checkOutput(BuildResult result, String imageName, String expectedOutput) {
        def imageBinDir = new File(testProjectDir.toFile(), 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''

        assert result.task(":$JlinkPlugin.TASK_NAME_JLINK").outcome in [TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE]

        def imageLauncher = new File(imageBinDir, "$imageName$launcherExt")
        assert imageLauncher.exists()
        assert imageLauncher.canExecute()

        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        def err = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, err)
        def outputText = out.toString()
        def errorText = err.toString()
        if (errorText) {
            System.out.println "[DEBUG_LOG] Launcher stderr: $errorText"
        }
        if (!outputText.trim()) {
            System.out.println "[DEBUG_LOG] Launcher stdout was empty. Exit code: ${process.exitValue()}"
        }
        assert outputText.trim() == expectedOutput

        true
    }
}

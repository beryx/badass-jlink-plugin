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

class JlinkPluginSpec extends Specification {
    String createBuildContent(String input, String output) {
        def inputCfg = input ? "jlinkInputProperty = \"$input\"" : ''
        def outputCfg = output ? "jlinkOutputProperty = file(\"$output\")" : ''
        def extension = (inputCfg || outputCfg) ?
            """
            jlink {
                $inputCfg
                $outputCfg
            }
            """ : ''
        return """
            plugins {
                id 'org.beryx.jlink'
            }
            $extension
        """.stripIndent()
    }

    def setUpBuild(String input = null, String output = null) {
        File buildFile = testProjectDir.newFile("build.gradle")
        buildFile.text = createBuildContent(input, output)
        println "Executing build script:\n${buildFile.text}"
    }

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    @Unroll
    def "should execute task with input=#input and output=#output"() {
        when:
        setUpBuild(input, output)
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME, "-is")
                .build();
        def expectedOutputFile = new File("$testProjectDir.root.path/build/$expectedOutputFilePath")

        then:
        result.task(":$JlinkPlugin.TASK_NAME").outcome == TaskOutcome.SUCCESS
        expectedOutputFile.text == "jlink: jlinkInputProperty = $expectedInputVal"

        where:
        input  | output                    || expectedInputVal                             | expectedOutputFilePath
        null   | null                      || 'jlinkInputProperty-default-val' | "jlink/jlink-out.txt"
        'val1' | null                      || 'val1'                                       | "jlink/jlink-out.txt"
        null   | '$buildDir/dir2/out2.txt' || 'jlinkInputProperty-default-val' | "dir2/out2.txt"
        'val3' | '$buildDir/dir3/out3.txt' || 'val3'                                       | "dir3/out3.txt"
    }

}

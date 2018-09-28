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
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.environment.OperatingSystem

class ShowProspectiveMergedModuleInfoSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    def cleanup() {
        println "CLEANUP"
    }

    def "should display the correct module-info for the merged module"() {
        given:
        new AntBuilder().copy( todir: testProjectDir.root ) {
            fileset( dir: 'src/test/resources/hello' )
        }
        File buildFile = new File(testProjectDir.root, "build.gradle")
        def outputWriter = new StringWriter(8192)

        when:
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .forwardStdOutput(outputWriter)
                .withProjectDir(buildFile.parentFile)
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_SHOW_PROSPECTIVE_MERGED_MODULE_INFO, "-is")
                .build();
        def task = result.task(":$JlinkPlugin.TASK_NAME_SHOW_PROSPECTIVE_MERGED_MODULE_INFO")

        then:
        task.outcome == TaskOutcome.SUCCESS
        // TODO
        // outputWriter.toString() == 'TODO'
    }

}

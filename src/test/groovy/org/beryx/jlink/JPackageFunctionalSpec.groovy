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
import spock.util.environment.OperatingSystem

class JPackageFunctionalSpec extends AbstractJlinkPluginTest {
    def "should support jpackage with secondary launchers"() {
        given:
        File buildFile = setUpBuild('multi-launch')
        buildFile << """
            jpackage {
            }
        """.stripIndent()

        when:
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('jpackageImage', '-is')
                .build();

        then:
        result.task(":jpackageImage").outcome == TaskOutcome.SUCCESS
    }

    def "should use launcher name instead of imageName for the main executable"() {
        given:
        setUpBuild('name-ignored')

        when:
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JPACKAGE_IMAGE, "-is")
                .build();

        then:
        result.task(":" + JlinkPlugin.TASK_NAME_JPACKAGE_IMAGE).outcome == TaskOutcome.SUCCESS

        def launcherExt = OperatingSystem.current.windows ? '.exe' : ''
        def wrongExecutablePath = OperatingSystem.current.macOs ? "build/jpackage/image.app/Contents/MacOS/image$launcherExt" : (OperatingSystem.current.windows ? "build/jpackage/image/image$launcherExt" : "build/jpackage/image/bin/image$launcherExt")
        def rightExecutablePath = OperatingSystem.current.macOs ? "build/jpackage/image.app/Contents/MacOS/hello$launcherExt" : (OperatingSystem.current.windows ? "build/jpackage/image/hello$launcherExt" : "build/jpackage/image/bin/hello$launcherExt")

        // This is the current BUGGY behavior: the executable is named "image" instead of "hello"
        assert !new File(testProjectDir.toFile(), wrongExecutablePath).exists()
        assert new File(testProjectDir.toFile(), rightExecutablePath).exists()
    }
}

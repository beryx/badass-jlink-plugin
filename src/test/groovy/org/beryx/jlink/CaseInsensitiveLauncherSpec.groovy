/*
 * Copyright 2026 the original author or authors.
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
import spock.lang.Specification

class CaseInsensitiveLauncherSpec extends AbstractJlinkPluginTest {
    def "should handle launcher name and imageName differing only by case"() {
        given:
        setUpBuild('case-insensitive-launchers')

        when:
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('jpackageImage', '-is')
                .build();
        System.out.println("[DEBUG_LOG] Build output:\n" + result.getOutput())

        then:
        result.task(":jpackageImage").outcome == TaskOutcome.SUCCESS
    }
}

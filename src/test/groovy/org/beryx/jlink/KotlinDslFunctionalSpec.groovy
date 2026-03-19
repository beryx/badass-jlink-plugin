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

class KotlinDslFunctionalSpec extends AbstractJlinkPluginTest {
    def "should support Kotlin-DSL"() {
        when:
        setUpBuild('kotlin-dsl')
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
        );

        then:
        checkOutput(result, 'hello', 'Hello, world!')
    }

    def "should create image of project with multiple launchers using kotlin DSL"() {
        when:
        setUpBuild('multi-launch-kotlin-dsl')
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withGradleVersion('8.14.4')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
        );

        then:
        checkOutput(result, 'hello', 'Hello, world!')
        checkOutput(result, 'helloAgain', 'Hello again!')
        checkOutput(result, 'howdy', 'Howdy!')
    }
}

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
import spock.lang.Ignore

class JlinkPluginFunctionalSpec extends AbstractJlinkPluginTest {

    def "should be compatible with the new JPMS features introduced in Gradle 6.4"() {
        when:
        setUpBuild('hello-logback', 'build.modular.gradle')
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'modular-hello', 'LOG: Hello, modular Java!')
    }

    def "should use configured toolchain"() {
        when:
        setUpBuild('hello-toolchain')
        def fromEnvCandidates = (['JAVA_HOME'] + System.getenv().keySet().findAll { it ==~ /JAVA_HOME_25_.+/ }).unique()
        def gradleArgs = [JlinkPlugin.TASK_NAME_JLINK, "-is"]
        if(fromEnvCandidates.size() > 1) {
            gradleArgs << "-Dorg.gradle.java.installations.fromEnv=${fromEnvCandidates.join(',')}"
        }
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(*gradleArgs)
                .build();

        then:
        checkOutput(result, 'hello-toolchain', 'line from: (30,80) to: (20,50)')
    }

    def "should adjust qualified opens in module-info"() {
        when:
        File buildFile = setUpBuild('opens-to-jaxb')
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
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
                .withDebug(false)
                .withGradleVersion('8.14.4')
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
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withGradleVersion('8.14.4')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
        );

        then:
        checkOutput(result, 'helloBom', '{"from":"Alice","to":"Bob","greeting":"Hello"}')
    }

    def "should create runtime image of project with annotations on module declaration"() {
        when:
        File buildFile = setUpBuild('hello-annotated-module')
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withGradleVersion('8.14.4')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'helloAnnotatedModule', 'Hello annotated module!')
    }

    def "should create image of project with multiple launchers"() {
        when:

        File buildFile = setUpBuild('multi-launch')
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withGradleVersion('8.14.4')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is")
                .build();

        then:
        checkOutput(result, 'hello', 'Hello, world!')
        checkOutput(result, 'helloAgain', 'Hello again!')
        checkOutput(result, 'howdy', 'Howdy!')
    }

    def "jlink task should be compatible with the gradle configuration cache"() {
        when:
        setUpHelloLogbackBuild(null, null, null, null)
        new File(testProjectDir.toFile(), "gradle.properties") << "org.gradle.jvmargs=--add-opens java.base/java.lang.invoke=ALL-UNNAMED"

        def runner = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "--configuration-cache", "--configuration-cache-problems=fail", "-is")

        BuildResult result1
        try {
            result1 = runner.build()
        } catch (Exception e) {
            System.out.println "[DEBUG_LOG] FAILED RESULT1 OUTPUT:\n" + e.buildResult.output
            throw e
        }
        System.out.println "[DEBUG_LOG] RESULT1 OUTPUT:\n${result1.output}"

        BuildResult result2 = runner.build()
        System.out.println "[DEBUG_LOG] RESULT2 OUTPUT:\n${result2.output}"

        then:
        result1.output.contains('Configuration cache entry stored')
        result2.output.contains('Reusing configuration cache')
        checkOutput(result2, 'modular-hello', 'LOG: Hello, modular Java!')
    }

    def "jpackage task should be compatible with the gradle configuration cache"() {
        when:
        setUpHelloLogbackBuild(null, null, null, null)
        new File(testProjectDir.toFile(), "gradle.properties") << "org.gradle.jvmargs=--add-opens java.base/java.lang.invoke=ALL-UNNAMED"

        def runner = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(JlinkPlugin.TASK_NAME_JPACKAGE, "--configuration-cache", "--configuration-cache-problems=fail", "-is")

        BuildResult result1
        try {
            result1 = runner.build()
        } catch (Exception e) {
            System.out.println "[DEBUG_LOG] FAILED RESULT1 OUTPUT:\n" + e.buildResult.output
            throw e
        }
        System.out.println "[DEBUG_LOG] RESULT1 OUTPUT:\n${result1.output}"

        BuildResult result2 = runner.build()
        System.out.println "[DEBUG_LOG] RESULT2 OUTPUT:\n${result2.output}"

        then:
        result1.output.contains('Configuration cache entry stored')
        result2.output.contains('Reusing configuration cache')
        checkOutput(result2, 'modular-hello', 'LOG: Hello, modular Java!')
    }

    def "jlinkZip task should be compatible with the gradle configuration cache"() {
        when:
        setUpHelloLogbackBuild(null, null, null, null)
        new File(testProjectDir.toFile(), "gradle.properties") << "org.gradle.jvmargs=--add-opens java.base/java.lang.invoke=ALL-UNNAMED"

        def runner = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments("clean", "build", JlinkPlugin.TASK_NAME_JLINK_ZIP, "--configuration-cache", "--configuration-cache-problems=fail", "-is")

        BuildResult result1 = runner.build()
        BuildResult result2 = runner.build()

        then:
        result1.output.contains('Configuration cache entry stored')
        result2.output.contains('Reusing configuration cache')
        checkOutput(result2, 'modular-hello', 'LOG: Hello, modular Java!')
    }

    def "suggestMergedModuleInfo task should be compatible with the gradle configuration cache"() {
        when:
        setUpHelloLogbackBuild(null, null, null, null)
        new File(testProjectDir.toFile(), "gradle.properties") << "org.gradle.jvmargs=--add-opens java.base/java.lang.invoke=ALL-UNNAMED"

        def runner = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.14.4')
                .withArguments(JlinkPlugin.TASK_NAME_SUGGEST_MERGED_MODULE_INFO, "--configuration-cache", "--configuration-cache-problems=fail", "-is")

        BuildResult result1 = runner.build()
        BuildResult result2 = runner.build()

        then:
        result1.output.contains('Configuration cache entry stored')
        result2.output.contains('Reusing configuration cache')
    }
}

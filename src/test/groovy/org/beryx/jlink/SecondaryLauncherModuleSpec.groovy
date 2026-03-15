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

class SecondaryLauncherModuleSpec extends AbstractJlinkPluginTest {
    def "should include secondary launcher module in jlink add-modules"() {
        when:
        setUpBuild('secondary-launcher-module')
        BuildResult result = GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion('8.5')
                .withArguments('jlink', "-is")
                .build()

        then:
        result.task(":jlink").outcome == TaskOutcome.SUCCESS

        // Find the jlink command in the output to check --add-modules
        def addModulesMatch = result.output =~ /--add-modules\s+([^\s]+)/
        assert addModulesMatch.find()
        def addModules = addModulesMatch.group(1).split(',')

        // Verify 'secondary' module is now included
        assert addModules.contains('secondary')

        // If we try to run the secondary launcher, it should now succeed
        def imageBinDir = new File(testProjectDir.toFile(), 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def secondaryLauncher = new File(imageBinDir, "secondary$launcherExt")

        assert secondaryLauncher.exists()

        def process = secondaryLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream()
        def err = new ByteArrayOutputStream()
        process.waitForProcessOutput(out, err)

        // It should succeed and print "Secondary"
        assert process.exitValue() == 0
        assert out.toString().contains('Secondary')
    }
}

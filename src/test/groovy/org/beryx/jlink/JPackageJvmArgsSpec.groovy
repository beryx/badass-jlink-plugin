package org.beryx.jlink

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.util.environment.OperatingSystem

class JPackageJvmArgsSpec extends AbstractJlinkPluginTest {
    def "should include applicationDefaultJvmArgs in jpackage image"() {
        given:
        File buildFile = setUpBuild('multi-launch')
        buildFile << """
            application {
                applicationDefaultJvmArgs = ["-Dmy.jvm.arg=my-value"]
            }
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

        def os = OperatingSystem.current
        def imageName = 'hello'
        def cfgPath
        if (os.windows) {
            cfgPath = "build/jpackage/$imageName/app/${imageName}.cfg"
        } else if (os.macOs) {
            cfgPath = "build/jpackage/${imageName}.app/Contents/app/${imageName}.cfg"
        } else {
            cfgPath = "build/jpackage/$imageName/lib/app/${imageName}.cfg"
        }

        def cfgFile = new File(testProjectDir.toFile(), cfgPath)
        assert cfgFile.exists()
        assert cfgFile.text.contains('java-options=-Dmy.jvm.arg=my-value')
    }

    def "secondary launchers should also include applicationDefaultJvmArgs"() {
        given:
        File buildFile = setUpBuild('multi-launch')
        buildFile << """
            application {
                applicationDefaultJvmArgs = ["-Dmy.secondary.arg=secondary-value"]
            }
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

        def os = OperatingSystem.current
        // 'helloAgain' is a secondary launcher defined in the 'multi-launch' project
        def launcherName = 'helloAgain'
        def cfgPath
        if (os.windows) {
            cfgPath = "build/jpackage/hello/app/${launcherName}.cfg"
        } else if (os.macOs) {
            cfgPath = "build/jpackage/hello.app/Contents/app/${launcherName}.cfg"
        } else {
            cfgPath = "build/jpackage/hello/lib/app/${launcherName}.cfg"
        }

        def cfgFile = new File(testProjectDir.toFile(), cfgPath)
        assert cfgFile.exists()
        assert cfgFile.text.contains('java-options=-Dmy.secondary.arg=secondary-value')
    }
}

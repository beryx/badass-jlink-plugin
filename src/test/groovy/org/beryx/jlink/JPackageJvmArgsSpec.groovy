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
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('jpackageImage', '-is'))

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
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('jpackageImage', '-is'))

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

    def "jpackage should prioritize launcher jvmArgs over applicationDefaultJvmArgs"() {
        given:
        File buildFile = setUpBuild('multi-launch')
        buildFile << """
            application {
                applicationDefaultJvmArgs = ["-Dapp.default.arg=app-value"]
            }
            jlink {
                launcher {
                    name = 'hello'
                    jvmArgs = ["-Dlauncher.arg=launcher-value"]
                }
            }
            jpackage {
            }
        """.stripIndent()

        when:
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments('jpackageImage', '-is'))

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
        // According to the docs: "defaultValue: the jvmArgs value configured in the launcher block,
        // or the arguments configured in the applicationDefaultJvmArgs property of the application extension."
        // This suggests launcher jvmArgs should be used if present.
        assert cfgFile.text.contains('java-options=-Dlauncher.arg=launcher-value')
        assert !cfgFile.text.contains('java-options=-Dapp.default.arg=app-value')
    }
}

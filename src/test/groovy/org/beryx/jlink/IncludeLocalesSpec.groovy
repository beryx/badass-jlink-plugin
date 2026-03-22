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

import org.beryx.jlink.data.CustomImageData
import org.beryx.jlink.data.JPackageData
import org.beryx.jlink.data.JPackageTaskData
import org.beryx.jlink.data.JlinkTaskData
import org.beryx.jlink.data.LauncherData
import org.beryx.jlink.impl.JPackageImageTaskImpl
import org.beryx.jlink.impl.JPackageTaskImpl
import org.beryx.jlink.impl.JlinkTaskImpl
import org.gradle.api.Action
import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class IncludeLocalesSpec extends Specification {
    @TempDir Path tmpDir

    def "jlink should add include-locales option when configured"() {
        given:
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jlink').toFile(), 'jlink')
        def jlinkJarsDir = tmpDir.resolve('jlink-jars').toFile()
        jlinkJarsDir.mkdirs()
        def imageDir = tmpDir.resolve('image').toFile()

        def taskData = new JlinkTaskData(
                javaHome: jdkHome.absolutePath,
                jlinkJarsDir: jlinkJarsDir,
                customImageData: new CustomImageData(),
                imageModules: ['java.base'],
                includeLocales: ['en', 'de'],
                options: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JlinkTaskImpl(fileSystemOperations, execOperations, taskData)
                .runJlink(imageDir, jdkHome.absolutePath, [], [])

        then:
        1 * fileSystemOperations.delete(_ as Action)
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        commandLine.containsAll(['--include-locales', 'en,de'])
        modulesArg(commandLine).contains('jdk.localedata')
    }

    def "jlink should not add include-locales option when not configured"() {
        given:
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jlink-none').toFile(), 'jlink')
        def jlinkJarsDir = tmpDir.resolve('jlink-jars-none').toFile()
        jlinkJarsDir.mkdirs()
        def imageDir = tmpDir.resolve('image-none').toFile()

        def taskData = new JlinkTaskData(
                javaHome: jdkHome.absolutePath,
                jlinkJarsDir: jlinkJarsDir,
                customImageData: new CustomImageData(),
                imageModules: ['java.base'],
                includeLocales: [],
                options: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JlinkTaskImpl(fileSystemOperations, execOperations, taskData)
                .runJlink(imageDir, jdkHome.absolutePath, [], [])

        then:
        1 * fileSystemOperations.delete(_ as Action)
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        !commandLine.contains('--include-locales')
        !modulesArg(commandLine).contains('jdk.localedata')
    }

    def "jlink should not duplicate jdk.localedata when already present"() {
        given:
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jlink-localedata').toFile(), 'jlink')
        def jlinkJarsDir = tmpDir.resolve('jlink-jars-localedata').toFile()
        jlinkJarsDir.mkdirs()
        def imageDir = tmpDir.resolve('image-localedata').toFile()

        def taskData = new JlinkTaskData(
                javaHome: jdkHome.absolutePath,
                jlinkJarsDir: jlinkJarsDir,
                customImageData: new CustomImageData(),
                imageModules: ['java.base', 'jdk.localedata'],
                includeLocales: ['en', 'de'],
                options: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JlinkTaskImpl(fileSystemOperations, execOperations, taskData)
                .runJlink(imageDir, jdkHome.absolutePath, [], [])

        then:
        1 * fileSystemOperations.delete(_ as Action)
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        def modules = modulesArg(commandLine).split(',').findAll()
        modules.count { it == 'jdk.localedata' } == 1
    }

    def "jpackage image should add include-locales option when configured"() {
        given:
        def projectDir = tmpDir.resolve('project-image').toFile()
        projectDir.mkdirs()
        def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jpackage-image').toFile(), 'jpackage')
        def runtimeImageDir = tmpDir.resolve('runtime-image').toFile()
        runtimeImageDir.mkdirs()

        def jpackageData = new JPackageData(project, new LauncherData('app'), project.layout.buildDirectory)
        jpackageData.jpackageHome = jdkHome.absolutePath
        jpackageData.includeLocales = ['en', 'de']

        def taskData = new JPackageTaskData(
                jlinkBasePath: tmpDir.resolve('jlinkbase').toFile().absolutePath,
                moduleName: 'com.example.app',
                mainClass: 'com.example.app.Main',
                customImageData: new CustomImageData(),
                runtimeImageDir: runtimeImageDir,
                projectVersion: '1.0.0',
                projectArchiveFile: tmpDir.resolve('app.jar').toFile(),
                jpackageData: jpackageData,
                defaultArgs: [],
                defaultJvmArgs: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JPackageImageTaskImpl(fileSystemOperations, execOperations, taskData).execute()

        then:
        1 * fileSystemOperations.delete(_ as Action)
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        commandLine.containsAll(['--include-locales', 'en,de'])
    }

    def "jpackage image should not add include-locales option when not configured"() {
        given:
        def projectDir = tmpDir.resolve('project-image-none').toFile()
        projectDir.mkdirs()
        def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jpackage-image-none').toFile(), 'jpackage')
        def runtimeImageDir = tmpDir.resolve('runtime-image-none').toFile()
        runtimeImageDir.mkdirs()

        def jpackageData = new JPackageData(project, new LauncherData('app'), project.layout.buildDirectory)
        jpackageData.jpackageHome = jdkHome.absolutePath

        def taskData = new JPackageTaskData(
                jlinkBasePath: tmpDir.resolve('jlinkbase-none').toFile().absolutePath,
                moduleName: 'com.example.app',
                mainClass: 'com.example.app.Main',
                customImageData: new CustomImageData(),
                runtimeImageDir: runtimeImageDir,
                projectVersion: '1.0.0',
                projectArchiveFile: tmpDir.resolve('app-none.jar').toFile(),
                jpackageData: jpackageData,
                defaultArgs: [],
                defaultJvmArgs: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JPackageImageTaskImpl(fileSystemOperations, execOperations, taskData).execute()

        then:
        1 * fileSystemOperations.delete(_ as Action)
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        !commandLine.contains('--include-locales')
    }

    def "jpackage installer should add include-locales option when configured"() {
        given:
        def projectDir = tmpDir.resolve('project-installer').toFile()
        projectDir.mkdirs()
        def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        def jdkHome = prepareFakeJdk(tmpDir.resolve('jdk-jpackage-installer').toFile(), 'jpackage')
        def appImageDir = tmpDir.resolve('project-installer/build/jpackage/app').toFile()
        appImageDir.mkdirs()

        def jpackageData = new JPackageData(project, new LauncherData('app'), project.layout.buildDirectory)
        jpackageData.jpackageHome = jdkHome.absolutePath
        jpackageData.installerType = 'deb'
        jpackageData.includeLocales = ['en', 'de']

        def taskData = new JPackageTaskData(
                jpackageData: jpackageData,
                projectVersion: '1.0.0',
                defaultArgs: [],
                defaultJvmArgs: []
        )

        List<String> commandLine = []
        def execOperations = Mock(ExecOperations)
        def fileSystemOperations = Mock(FileSystemOperations)
        def execResult = dummyExecResult()

        when:
        new JPackageTaskImpl(fileSystemOperations, execOperations, taskData).execute()

        then:
        1 * execOperations.exec(_ as Action) >> { Action action ->
            def spec = new Expando()
            action.execute(spec)
            commandLine = spec.commandLine as List<String>
            execResult
        }
        commandLine.containsAll(['--include-locales', 'en,de'])
    }

    private static ExecResult dummyExecResult() {
        [
                getExitValue: { -> 0 },
                assertNormalExitValue: { -> null },
                rethrowFailure: { -> null }
        ] as ExecResult
    }

    private static String modulesArg(List<String> commandLine) {
        def idx = commandLine.indexOf('--add-modules')
        if(idx < 0 || idx + 1 >= commandLine.size()) return ''
        commandLine[idx + 1]
    }

    private static File prepareFakeJdk(File jdkHome, String executable) {
        def binDir = new File(jdkHome, 'bin')
        def jmodsDir = new File(jdkHome, 'jmods')
        binDir.mkdirs()
        jmodsDir.mkdirs()
        new File(jmodsDir, 'java.base.jmod').text = ''
        def tool = new File(binDir, "$executable${org.beryx.jlink.util.Util.EXEC_EXTENSION}")
        tool.text = 'echo'
        tool.setExecutable(true)
        jdkHome
    }
}

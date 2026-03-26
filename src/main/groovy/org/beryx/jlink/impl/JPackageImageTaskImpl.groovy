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
package org.beryx.jlink.impl

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jlink.data.JPackageTaskData
import org.beryx.jlink.data.SecondaryLauncherData
import org.beryx.jlink.util.Util
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations

import java.nio.file.Files

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JPackageImageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTaskImpl.class);

    private final FileSystemOperations fileSystemOperations
    private final ExecOperations execOperations

    JPackageImageTaskImpl(FileSystemOperations fileSystemOperations, ExecOperations execOperations, JPackageTaskData taskData) {
        super(taskData)
        this.fileSystemOperations = fileSystemOperations
        this.execOperations = execOperations
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        def jpd = td.jpackageData
        fileSystemOperations.delete { it.delete(jpd.imageOutputDir) }
        def result = {
            def outputStream = new ByteArrayOutputStream()

            def execResult = execOperations.exec { spec ->
                spec.ignoreExitValue = true
                spec.standardOutput = outputStream
                spec.commandLine = createCommandLine()
            }

            def jpackageImageOutput = outputStream.toString()

            return [execResult: execResult, output: jpackageImageOutput]
        }()
        def execResult = result.execResult as org.gradle.process.ExecResult
        def output = result.output as String
        if (execResult.exitValue != 0) {
            LOGGER.error(output)
        } else {
            LOGGER.info(output)
            deleteDefaultLauncher()
        }
        Util.cleanupTempFiles(jpd.imageOutputDir)
        execResult.assertNormalExitValue()
        execResult.rethrowFailure()
    }

    private void deleteDefaultLauncher() {
        def jpd = td.jpackageData
        if (jpd.launcherName && !jpd.launcherName.equalsIgnoreCase(jpd.imageName)) {
            def imageOutputDir = jpd.imageOutputDir
            def imageName = jpd.imageName
            def os = org.gradle.internal.os.OperatingSystem.current()
            def launcherExt = os.windows ? '.exe' : ''
            File defaultLauncher
            if (os.macOsX) {
                defaultLauncher = new File(imageOutputDir, "${imageName}.app/Contents/MacOS/${imageName}${launcherExt}")
            } else if (os.windows) {
                defaultLauncher = new File(imageOutputDir, "${imageName}/${imageName}${launcherExt}")
            } else {
                defaultLauncher = new File(imageOutputDir, "${imageName}/bin/${imageName}${launcherExt}")
            }
            if (defaultLauncher.exists()) {
                LOGGER.info("Deleting default launcher: $defaultLauncher")
                defaultLauncher.delete()
            }
        }
    }

    private List<String> createCommandLine() {
        def jpd = td.jpackageData
        def jpackageExec = "${jpd.getJPackageHomeOrDefault()}/bin/jpackage$EXEC_EXTENSION" as String
        Util.checkExecutable(jpackageExec)

        Map<String,File> propFiles = [:]
        jpd.secondaryLaunchers.get().each { SecondaryLauncherData launcher ->
            def propFile = createPropFile(launcher.name, launcher.moduleName, launcher.mainClass,
                    launcher.getEffectiveArgs(td.defaultArgs), launcher.getEffectiveJvmArgs(td.defaultJvmArgs),
                    launcher.appVersion, launcher.icon, launcher.winConsole, propFiles)
            propFiles[launcher.name] = propFile
        }
        if (jpd.launcherName && !jpd.launcherName.equalsIgnoreCase(jpd.imageName)) {
            def propFile = createPropFile(jpd.launcherName, td.moduleName, td.mainClass,
                    jpd.args.get(), jpd.jvmArgs.get(),
                    jpd.getAppVersion(), jpd.getIcon(), null, propFiles)
            propFiles[jpd.launcherName] = propFile
        }

        def appVersion = (jpd.appVersion ?: td.projectVersion).toString()
        List<String> versionOpts = (appVersion == 'unspecified') ? [] : [ '--app-version', appVersion ]
        if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
            throw new GradleException("The first character of the --app-version argument should be a digit.")
        }

        List<String> iconOpts = jpd.icon ? [ '--icon', jpd.icon ] : []

        final File resourceDir = jpd.getResourceDir()
        final List<String> resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir.absolutePath ]

        List<String> moduleOrJarOpts = []
        if (td.customImageData.enabled) {
            def appDir = new File(td.runtimeImageDir, 'app')
            appDir.mkdirs()
            moduleOrJarOpts += ['--input', appDir.absolutePath]
            String mainJarName = td.projectArchiveFile.name
            def mainJar = new File(appDir, mainJarName)
            LOGGER.info("mainJar $mainJar ${mainJar.file ? '' : 'not '}found")
            if(mainJar.file) {
                moduleOrJarOpts += [
                        '--main-jar', mainJar.name,
                        '--main-class', td.mainClass
                ]
            } else {
                moduleOrJarOpts += ['--module', "$td.moduleName/$td.mainClass" as String]
            }
        } else {
            moduleOrJarOpts += ['--module', "$td.moduleName/$td.mainClass" as String]
        }
        return createCommandLine(jpackageExec, moduleOrJarOpts, versionOpts, iconOpts, resourceOpts, propFiles)
    }

    @CompileDynamic
    private List<String> createCommandLine(
            String jpackageExec,
            List<String> moduleOrJarOpts,
            List<String> versionOpts,
            List<String> iconOpts,
            List<String> resourceOpts,
            Map<String, File> propFiles) {
        def jpd = td.jpackageData
        return [
            jpackageExec,
            '--type', 'app-image',
            '--dest', td.jpackageData.imageOutputDir,
            '--name', jpd.imageName,
            *moduleOrJarOpts,
            *versionOpts,
            *iconOpts,
            '--runtime-image', td.runtimeImageDir,
            *resourceOpts,
            *(jpd.jvmArgs.get() ? jpd.jvmArgs.get().collect { ['--java-options', adjustArg(it)] }.flatten() : []),
            *(jpd.args.get() ? jpd.args.get().collect { ['--arguments', adjustArg(it)] }.flatten() : []),
            *(propFiles ? propFiles.collect { ['--add-launcher', it.key + '=' + it.value.absolutePath] }.flatten() : []),
            *jpd.imageOptions.get()
        ]
    }

    private File createPropFile(String name, String moduleName, String mainClass, List<String> args, List<String> jvmArgs, String appVersion, String icon, Boolean winConsole, Map<String, File> propFiles) {
        def propFile = new File("$td.jlinkBasePath/${name}.properties")
        Files.deleteIfExists(propFile.toPath())
        propFile.withOutputStream { stream ->
            if (moduleName) {
                stream << "module=$moduleName\n"
            }
            if (mainClass) {
                stream << "main-class=$mainClass\n"
            }
            if (args) {
                stream << "arguments=${args.collect { adjustArg(it) }.join('\\n')}\n"
            }
            if (jvmArgs) {
                stream << "java-options=${jvmArgs.collect { adjustArg(it) }.join('\\n')}\n"
            }
            if (appVersion) {
                stream << "app-version=$appVersion\n"
            }
            if (icon) {
                stream << "icon=$icon\n"
            }
            if (winConsole != null) {
                stream << "win-console=$winConsole\n"
            }
        }
        return propFile
    }

    static String adjustArg(String arg) {
        def adjusted = arg.replace('"', '\\"')
        if(!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8227641
        adjusted = adjusted.replace(' ', '\\" \\"')
        adjusted = adjusted.replace('{{BIN_DIR}}', '$APPDIR' + File.separator + '..')
        adjusted
    }
}

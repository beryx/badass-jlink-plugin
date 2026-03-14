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
        fileSystemOperations.delete { it.delete(td.jpackageData.imageOutputDir) }
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
        if(execResult.exitValue != 0) {
            LOGGER.error(output)
        } else {
            LOGGER.info(output)
        }
        execResult.assertNormalExitValue()
        execResult.rethrowFailure()
    }

    private List<String> createCommandLine() {
        def jpd = td.jpackageData
        def jpackageExec = "${jpd.getJPackageHomeOrDefault()}/bin/jpackage$EXEC_EXTENSION" as String
        Util.checkExecutable(jpackageExec)

        Map<String,File> propFiles = [:]
        jpd.secondaryLaunchers.each { SecondaryLauncherData launcher ->
            def propFile = new File("$td.jlinkBasePath/${launcher.name}.properties")
            Files.deleteIfExists(propFile.toPath())
            propFile.withOutputStream { stream ->
                if(launcher.moduleName) {
                    stream << "module=$launcher.moduleName\n"
                }
                if(launcher.mainClass) {
                    stream << "main-class=$launcher.mainClass\n"
                }

                def args = launcher.getEffectiveArgs(td.defaultArgs)
                if(args) {
                    stream << "arguments=${args.collect{adjustArg(it)}.join('\\n')}\n"
                }

                def jvmArgs = launcher.getEffectiveJvmArgs(td.defaultJvmArgs)
                if(jvmArgs) {
                    stream << "java-options=${jvmArgs.collect{adjustArg(it)}.join('\\n')}\n"
                }
                if(launcher.appVersion) {
                    stream << "app-version=$launcher.appVersion\n"
                }
                if(launcher.icon) {
                    stream << "icon=$launcher.icon\n"
                }
                if(launcher.winConsole != null) {
                    stream << "win-console=$launcher.winConsole\n"
                }
            }
            propFiles[launcher.name] = propFile
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
            *(jpd.jvmArgs ? jpd.jvmArgs.collect { ['--java-options', adjustArg(it)] }.flatten() : []),
            *(jpd.args ? jpd.args.collect { ['--arguments', adjustArg(it)] }.flatten() : []),
            *(propFiles ? propFiles.collect { ['--add-launcher', it.key + '=' + it.value.absolutePath] }.flatten() : []),
            *jpd.imageOptions
        ]
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

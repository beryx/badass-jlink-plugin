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

import java.nio.file.Files
import java.nio.file.Paths
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jlink.data.JPackageTaskData
import org.beryx.jlink.util.Util
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JPackageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageTaskImpl.class);

    private final FileSystemOperations fileSystemOperations
    private final ExecOperations execOperations

    JPackageTaskImpl(FileSystemOperations fileSystemOperations, ExecOperations execOperations, JPackageTaskData taskData) {
        super(taskData)
        this.fileSystemOperations = fileSystemOperations
        this.execOperations = execOperations
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        def jpd = td.jpackageData
        if(jpd.skipInstaller) {
            LOGGER.info("Skipping create-installer")
            return
        }
        def appImagePath = "$jpd.imageOutputDir/$jpd.imageName"
        def os = OperatingSystem.current()
        if(os.windows && !jpd.imageName.equalsIgnoreCase(jpd.installerName)) { // Workaround for https://github.com/beryx/badass-jlink-plugin/issues/169
            def appImageExe = Paths.get(appImagePath, "${jpd.imageName}.exe")
            def newAppImageExe = Paths.get(appImagePath, "${jpd.installerName}.exe")
            LOGGER.info "Copying $appImageExe into $newAppImageExe"
            Files.copy(appImageExe, newAppImageExe)
        }
        if(os.macOsX) {
            def appImageDir = new File(appImagePath)
            if(!appImageDir.directory) {
                def currImagePath = "$jpd.imageOutputDir/${jpd.imageName}.app"
                if(!new File(currImagePath).directory) {
                    throw new GradleException("Unable to find the application image in $jpd.imageOutputDir")
                }
                appImagePath = currImagePath
            }
        }

        if (jpd.imageOutputDir != jpd.installerOutputDir) {
            fileSystemOperations.delete { it.delete(jpd.installerOutputDir) }
        }
        packageTypes.each { packageType ->
            def result = {
                def outputStream = new ByteArrayOutputStream()

                def jpackageExec = "${jpd.getJPackageHomeOrDefault()}/bin/jpackage$EXEC_EXTENSION"
                Util.checkExecutable(jpackageExec)

                def appVersion = (jpd.appVersion ?: td.projectVersion).toString()
                def versionOpts = (appVersion == 'unspecified') ? [] : [ '--app-version', appVersion ]
                if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                    throw new GradleException("The first character of the --app-version argument should be a digit.")
                }

                def vendor = jpd.getVendor()
                def vendorOpts = ['--vendor', vendor]

                final def resourceDir = jpd.resourceDir
                final def resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir ]
                final def iconOpts = jpd.icon ? [ '--icon', jpd.icon ] : []

                def execResult = execOperations.exec { spec ->
                    spec.ignoreExitValue = true
                    spec.standardOutput = outputStream
                    spec.commandLine = [
                            jpackageExec,
                            '--type', packageType,
                            '--dest', jpd.installerOutputDir,
                            '--name', jpd.installerName,
                            *versionOpts,
                            *vendorOpts,
                            '--app-image', "$appImagePath",
                            *resourceOpts,
                            *iconOpts,
                            *includeLocalesOption(jpd.effectiveIncludeLocales, jpd.installerOptions.get()),
                            *jpd.installerOptions.get()
                    ]
                }

                def jpackageInstallerOutput = outputStream.toString()

                return [execResult: execResult, output: jpackageInstallerOutput]
            }()
            def execResult = result.execResult as org.gradle.process.ExecResult
            def output = result.output as String
            if(execResult.exitValue != 0) {
                LOGGER.error(output)
            } else {
                LOGGER.info(output)
                if(os.windows && !jpd.imageName.equalsIgnoreCase(jpd.installerName)) { // Workaround for https://github.com/beryx/badass-jlink-plugin/issues/169
                    new File("$appImagePath/${jpd.installerName}.exe").delete()
                }
            }
            Util.cleanupTempFiles(jpd.installerOutputDir)
            Util.cleanupTempFiles(jpd.imageOutputDir)
            execResult.assertNormalExitValue()
            execResult.rethrowFailure()
        }
    }

    List<String> getPackageTypes() {
        def jpd = td.jpackageData
        if(jpd.installerType) return [jpd.installerType]
        if(OperatingSystem.current().windows) {
            return ['exe', 'msi']
        } else if(OperatingSystem.current().macOsX) {
            return ['pkg', 'dmg']
        } else {
            return ['rpm', 'deb']
        }
    }

    private static List<String> includeLocalesOption(List<String> includeLocales, List<String> existingOptions) {
        if(hasIncludeLocalesOption(existingOptions)) return []
        if(!includeLocales) return []
        ['--include-locales', includeLocales.join(',')]
    }

    private static boolean hasIncludeLocalesOption(List<String> options) {
        options.any { it?.startsWith('--include-locales') }
    }
}

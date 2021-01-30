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
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.os.OperatingSystem

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JPackageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageTaskImpl.class);

    JPackageTaskImpl(Project project, JPackageTaskData taskData) {
        super(project, taskData)
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
        if(os.windows && jpd.imageName != jpd.installerName) { // Workaround for https://github.com/beryx/badass-jlink-plugin/issues/169
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
            project.delete(project.files(jpd.installerOutputDir))
        }
        packageTypes.each { packageType ->
            def result = project.exec {
                ignoreExitValue = true
                standardOutput = new ByteArrayOutputStream()
                project.ext.jpackageInstallerOutput = {
                    return standardOutput.toString()
                }
                def jpackageExec = "${jpd.getJPackageHomeOrDefault()}/bin/jpackage$EXEC_EXTENSION"
                Util.checkExecutable(jpackageExec)

                def appVersion = (jpd.appVersion ?: project.version).toString()
                def versionOpts = (appVersion == Project.DEFAULT_VERSION) ? [] : [ '--app-version', appVersion ]
                if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                    throw new GradleException("The first character of the --app-version argument should be a digit.")
                }

                final def resourceDir = jpd.resourceDir
                final def resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir ]

                commandLine = [jpackageExec,
                               '--type', packageType,
                               '--dest', jpd.installerOutputDir,
                               '--name', jpd.installerName,
                               *versionOpts,
                               '--app-image', "$appImagePath",
                               *resourceOpts,
                               *jpd.installerOptions]
            }
            if(result.exitValue != 0) {
                LOGGER.error(project.ext.jpackageInstallerOutput())
            } else {
                LOGGER.info(project.ext.jpackageInstallerOutput())
                if(os.windows && jpd.imageName != jpd.installerName) { // Workaround for https://github.com/beryx/badass-jlink-plugin/issues/169
                    new File("$appImagePath/${jpd.installerName}.exe").delete()
                }
            }
            result.assertNormalExitValue()
            result.rethrowFailure()
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
}

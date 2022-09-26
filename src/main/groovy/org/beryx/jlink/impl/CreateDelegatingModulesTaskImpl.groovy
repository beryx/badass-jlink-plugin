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

import org.beryx.jlink.util.Util
import org.beryx.jlink.data.CreateDelegatingModulesTaskData
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CreateDelegatingModulesTaskImpl extends BaseTaskImpl<CreateDelegatingModulesTaskData> {
    private static final Logger LOGGER = Logging.getLogger(CreateDelegatingModulesTaskImpl.class);

    CreateDelegatingModulesTaskImpl(Project project, CreateDelegatingModulesTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        LOGGER.info("Creating delegating modules...")
        project.delete(td.tmpJarsDirPath)

        td.nonModularJarsDir.eachFile { jarFile ->
            createDelegatingModule(jarFile, td.tmpJarsDirPath, td.delegatingModulesDir)
        }
    }

    def createDelegatingModule(File jarFile, String tmpDirPath, File targetDir) {
        def moduleDir = genDelegatingModuleInfo(jarFile, tmpDirPath)
        if(!moduleDir) return
        project.delete(td.tmpModuleInfoDirPath)
        Util.createManifest(td.tmpModuleInfoDirPath, false)
        LOGGER.info("Compiling delegating module $moduleDir.name ...")
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.javacOutput = {
                return standardOutput.toString()
            }
            commandLine "$td.javaHome/bin/javac",
                    '-p',
                    td.jlinkJarsDirPath,
                    '-d',
                    td.tmpModuleInfoDirPath,
                    "${moduleDir.path}/module-info.java"
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.javacOutput())
        } else {
            LOGGER.info(project.ext.javacOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()

        def targetJarPath = new File(targetDir, jarFile.name).path
        Util.createJar(project, targetJarPath, project.file(td.tmpModuleInfoDirPath))
    }

    File genDelegatingModuleInfo(File jarFile, String targetDirPath) {
        def moduleName = Util.getModuleName(jarFile)
        def modinfoDir = new File(targetDirPath, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        if(modInfoJava.exists()) {
            LOGGER.info("Module $moduleName already generated. Skipping $jarFile")
            return null
        }
        LOGGER.info("Generating module $moduleName for: $jarFile")
        modInfoJava << """
            open module $moduleName {
                requires transitive $td.mergedModuleName;
            }
            """.stripIndent()
        modinfoDir
    }
}

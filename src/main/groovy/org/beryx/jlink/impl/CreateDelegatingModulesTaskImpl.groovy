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

class CreateDelegatingModulesTaskImpl extends BaseTaskImpl<CreateDelegatingModulesTaskData> {
    CreateDelegatingModulesTaskImpl(Project project, CreateDelegatingModulesTaskData taskData) {
        super(project, taskData)
        project.logger.info("taskData: $taskData")
    }

    void execute() {
        project.logger.info("Creating delegating modules...")
        project.delete(td.tmpJarsDirPath)

        td.nonModularJarsDir.eachFile { jarFile ->
            createDelegatingModule(jarFile, td.tmpJarsDirPath, td.delegatingModulesDir)
        }
    }

    def createDelegatingModule(File jarFile, String tmpDirPath, File targetDir) {
        def moduleDir = genDelegatingModuleInfo(jarFile, tmpDirPath)
        project.delete(td.tmpModuleInfoDirPath)
        Util.createManifest(td.tmpModuleInfoDirPath, false)
        project.logger.info("Compiling delegating module $moduleDir.name ...")
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
            project.logger.error(project.ext.javacOutput())
        } else {
            project.logger.info(project.ext.javacOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()

        def targetJarPath = new File(targetDir, jarFile.name).path
        Util.createJar(project, td.javaHome, targetJarPath, td.tmpModuleInfoDirPath)
    }

    File genDelegatingModuleInfo(File jarFile, String targetDirPath) {
        def moduleName = Util.getModuleName(jarFile, project)
        def modinfoDir = new File(targetDirPath, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava << """
        open module $moduleName {
            requires transitive $td.mergedModuleName;
        }
        """.stripIndent()
        modinfoDir
    }
}

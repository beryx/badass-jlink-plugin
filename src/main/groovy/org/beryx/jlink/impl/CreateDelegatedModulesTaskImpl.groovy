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
import org.beryx.jlink.data.CreateDelegatedModulesTaskData
import org.gradle.api.Project

class CreateDelegatedModulesTaskImpl extends BaseTaskImpl<CreateDelegatedModulesTaskData> {
    CreateDelegatedModulesTaskImpl(Project project, CreateDelegatedModulesTaskData taskData) {
        super(project, taskData)
    }

    void execute() {
        project.logger.info("Creating delegated modules...")
        project.delete(td.tmpJarsDirPath)

        td.nonModularJarsDir.eachFile { jarFile ->
            createDelegatedModule(jarFile, td.tmpJarsDirPath, td.delegatedModulesDir)
        }
    }

    def createDelegatedModule(File jarFile, String tmpDirPath, File targetDir) {
        def moduleDir = genDelegatedModuleInfo(jarFile, tmpDirPath)
        project.delete(td.tmpModuleInfoDirPath)
        Util.createManifest(td.tmpModuleInfoDirPath)
        project.logger.info("Compiling delegate module $moduleDir.name ...")
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

    File genDelegatedModuleInfo(File jarFile, String targetDirPath) {
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

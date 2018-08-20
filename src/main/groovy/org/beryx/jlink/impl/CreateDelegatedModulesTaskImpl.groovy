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
import org.beryx.jlink.taskdata.CreateDelegatedModulesTaskData
import org.gradle.api.Project

class CreateDelegatedModulesTaskImpl extends BaseTaskImpl {
    final String mergedModuleName
    final String javaHome

    CreateDelegatedModulesTaskImpl(Project project, CreateDelegatedModulesTaskData taskData) {
        super(project)

        this.mergedModuleName = taskData.mergedModuleName
        this.javaHome = taskData.javaHome
    }

    void execute() {
        project.logger.info("Creating delegated modules...")
        project.delete(tmpJarsDirPath)
        new File(nonModularJarsDirPath).eachFile { jarFile ->
            createDelegatedModule(jarFile, tmpJarsDirPath, jlinkJarsDirPath)
        }
    }

    File genDelegatedModuleInfo(File jarFile, String targetDirPath) {
        def moduleName = Util.getModuleName(jarFile, project)
        def modinfoDir = new File(targetDirPath, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava << """
        open module $moduleName {
            requires transitive $mergedModuleName;
        }
        """.stripIndent()
        modinfoDir
    }

    def createJar(String jarFilePath, String contentDirPath) {
        project.file(jarFilePath).parentFile.mkdirs()
        project.exec {
            commandLine "$javaHome/bin/jar",
                    '--create',
                    '--file',
                    jarFilePath,
                    '-C',
                    contentDirPath,
                    '.'
        }
    }

    def createDelegatedModule(File jarFile, String tmpDirPath, String targetDirPath) {
        def moduleDir = genDelegatedModuleInfo(jarFile, tmpDirPath)
        project.delete(tmpModuleInfoDirPath)
        createManifest(tmpModuleInfoDirPath)
        project.logger.info("Compiling delegate module $moduleDir.name ...")
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.javacOutput = {
                return standardOutput.toString()
            }
            commandLine "$javaHome/bin/javac",
                    '-p',
                    jlinkJarsDirPath,
                    '-d',
                    tmpModuleInfoDirPath,
                    "${moduleDir.path}/module-info.java"
        }
        if(result.exitValue != 0) {
            project.logger.error(project.ext.javacOutput())
        } else {
            project.logger.info(project.ext.javacOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()

        def targetJarPath = new File(targetDirPath, jarFile.name).path
        createJar(targetJarPath, tmpModuleInfoDirPath)
    }

    def createManifest(String targetDirPath) {
        def mfdir = new File(targetDirPath, 'META-INF')
        mfdir.mkdirs()
        def mf = new File(mfdir, 'MANIFEST.MF')
        mf.delete()
        mf << """
        Manifest-Version: 1.0
        Created-By: Badass-JLink Plugin
        Built-By: gradle
        """.stripMargin()
    }
}

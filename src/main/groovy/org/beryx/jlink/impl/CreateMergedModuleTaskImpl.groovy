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
import org.beryx.jlink.data.CreateMergedModuleTaskData
import org.gradle.api.Project

import java.util.zip.ZipFile

class CreateMergedModuleTaskImpl extends BaseTaskImpl<CreateMergedModuleTaskData> {
    CreateMergedModuleTaskImpl(Project project, CreateMergedModuleTaskData taskData) {
        super(project, taskData)
        project.logger.info("taskData: $taskData")
    }

    void execute() {
        def jarFilePath = "$td.tmpMergedModuleDirPath/${td.mergedModuleName}.jar"
        Util.createJar(project, td.javaHome, jarFilePath, td.mergedJarsDir)
        def modInfoDir = genModuleInfo(project.file(jarFilePath), project.file(td.tmpJarsDirPath))
        compileModuleInfo(project.file(modInfoDir), project.file(jarFilePath), project.file(td.tmpModuleInfoDirPath))
        project.logger.info("Copy from $jarFilePath into ${td.mergedModuleJar}...")
        project.copy {
            from jarFilePath
            into td.jlinkJarsDirPath
        }
        project.logger.info("Insert module-info from $td.tmpModuleInfoDirPath into ${td.mergedModuleJar}...")
        insertModuleInfo(td.mergedModuleJar, project.file(td.tmpModuleInfoDirPath))
    }

    File genModuleInfo(File jarFile, File targetDir) {
        project.logger.info("Generating module-info in ${targetDir}...")
        project.delete(targetDir)
        if(td.jdepsEnabled) {
            project.exec {
                ignoreExitValue = true
                commandLine "$td.javaHome/bin/jdeps",
                        '-v',
                        '--generate-module-info',
                        targetDir.path,
                        '--module-path',
                        "$td.javaHome/jmods/$SEP$td.jlinkJarsDirPath",
                        jarFile.path
            }
        }
        def files = targetDir.listFiles()
        return files?.length ? files[0] : genDummyModuleInfo(jarFile, targetDir)
    }

    File genDummyModuleInfo(File jarFile, File targetDir) {
        def packages = new TreeSet<String>()
        new ZipFile(jarFile).entries().each { entry ->
            def pkgName = Util.getPackage(entry.name)
            if(pkgName) packages << pkgName
        }
        def moduleName = Util.getModuleName(jarFile, project)
        def modinfoDir = new File(targetDir, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava << "open module $moduleName {\n"
        packages.each {
            modInfoJava << "    exports $it;\n"
        }
        modInfoJava << td.mergedModuleInfo.toString(4) << '\n}\n'

        modinfoDir
    }

    def compileModuleInfo(File moduleInfoJavaDir, File moduleJar, File targetDir) {
        project.logger.info("Compiling module-info from ${moduleInfoJavaDir}...")
        project.delete(targetDir)
        project.copy {
            from(project.zipTree(moduleJar))
            into(targetDir)
        }
        project.exec {
            commandLine "$td.javaHome/bin/javac",
                    '-p',
                    "$moduleJar.parentFile$SEP$td.jlinkJarsDirPath",
                    '-d',
                    targetDir.path,
                    "$moduleInfoJavaDir/module-info.java"
        }
    }

    def insertModuleInfo(File moduleJar, File moduleInfoClassDir) {
        project.logger.info("Inserting module-info into ${moduleJar}...")
        project.exec {
            commandLine "$td.javaHome/bin/jar",
                    '--update',
                    '--file',
                    moduleJar.path,
                    '-C',
                    moduleInfoClassDir.path,
                    'module-info.class'
        }
    }
}

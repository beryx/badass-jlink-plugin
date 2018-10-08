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

import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.util.JdepsExecutor
import org.beryx.jlink.util.SuggestedMergedModuleInfoBuilder
import org.beryx.jlink.util.Util
import org.beryx.jlink.data.CreateMergedModuleTaskData
import org.gradle.api.GradleException
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
        targetDir.mkdirs()
        def moduleInfoFile = genModuleInfoJdeps(jarFile, targetDir)
        if(!moduleInfoFile) {
            moduleInfoFile = genModuleInfoBadass(jarFile, targetDir)
        }
        moduleInfoFile
    }

    File genModuleInfoJdeps(File jarFile, File targetDir) {
        if(td.useJdeps != JdepsUsage.no) {
            def result = new JdepsExecutor(project).genModuleInfo(jarFile, targetDir, td.jlinkJarsDirPath, td.javaHome)
            if(result.exitValue) {
                if(td.useJdeps != JdepsUsage.exclusively) {
                    throw new GradleException("jdeps exited with return code $result.exitValue")
                }
            } else {
                def files = targetDir.listFiles{File dir, String name -> name == 'module-info.java'}
                if(files?.length) return files[0]
                project.logger.warn("jdeps terminated successfully but the module declaration file cannot be found.")
            }
        }
        null
    }

    File genModuleInfoBadass(File jarFile, File targetDir) {
        def packages = new TreeSet<String>()
        new ZipFile(jarFile).entries().each { entry ->
            def pkgName = Util.getPackage(entry.name)
            if(pkgName) packages << pkgName
        }
        def moduleName = Util.getModuleName(jarFile, project)
        def modinfoDir = new File(targetDir, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava.delete()
        modInfoJava << "open module $moduleName {\n"
        packages.each {
            modInfoJava << "    exports $it;\n"
        }
        if(td.mergedModuleInfo.enabled) {
            modInfoJava << td.mergedModuleInfo.toString(4)
        } else {
            def builder = new SuggestedMergedModuleInfoBuilder(project, td.mergedJarsDir, td.javaHome, td.forceMergedJarPrefixes)
            modInfoJava << builder.moduleInfo.toString(4)
        }
        modInfoJava << '\n}\n'
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

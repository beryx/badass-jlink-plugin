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

import org.beryx.jlink.taskdata.ModuleInfo
import org.beryx.jlink.util.Util
import org.beryx.jlink.taskdata.CreateMergedModuleTaskData
import org.gradle.api.Project

import java.util.zip.ZipFile

class CreateMergedModuleTaskImpl extends BaseTaskImpl {
    final String mergedModuleName
    final List<String> forceMergedJarPrefixes
    final String javaHome
    final boolean jdepsEnabled

    final ModuleInfo mergedModuleInfo

    CreateMergedModuleTaskImpl(Project project, CreateMergedModuleTaskData taskData) {
        super(project)

        this.mergedModuleName = taskData.mergedModuleName
        this.forceMergedJarPrefixes = taskData.forceMergedJarPrefixes
        this.javaHome = taskData.javaHome
        this.mergedModuleInfo = taskData.mergedModuleInfo
        this.jdepsEnabled = taskData.jdepsEnabled
    }

    void execute() {
        project.delete(jlinkBasePath)
        def depMgr = new DependencyManager(project, forceMergedJarPrefixes)
        copyRuntimeJars(depMgr)
        createMergedModule(new File(nonModularJarsDirPath).listFiles() as List)
    }

    File genModuleInfo(File jarFile, File targetDir) {
        project.logger.info("Generating module-info in ${targetDir}...")
        project.delete(targetDir)
        if(jdepsEnabled) {
            project.exec {
                ignoreExitValue = true
                commandLine "$javaHome/bin/jdeps",
                        '-v',
                        '--generate-module-info',
                        targetDir.path,
                        '--module-path',
                        "$javaHome/jmods/$SEP$jlinkJarsDirPath",
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
        modInfoJava << mergedModuleInfo.toString(4) << '\n}\n'

        modinfoDir
    }

    def copyRuntimeJars(DependencyManager depMgr) {
        project.delete(jlinkJarsDirPath, nonModularJarsDirPath)
        project.logger.info("Copying modular jars required by non-modular jars to ${jlinkJarsDirPath}...")
        depMgr.modularJarsRequiredByNonModularJars.each { jar ->
            project.logger.debug("\t... from $jar ...")
            project.copy {
                into jlinkJarsDirPath
                from jar
            }
        }
        project.logger.info("Copying mon-modular jars to ${nonModularJarsDirPath}...")
        depMgr.nonModularJars.each { jar ->
            project.copy {
                into nonModularJarsDirPath
                from jar
            }
        }
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

    def createMergedModule(Collection<File> jars) {
        if(jars.empty) return
        project.logger.info("Creating merged module...")
        mergeUnpackedContents(jars, mergedJarsDirPath)
        def jarFilePath = "$tmpMergedModuleDirPath/${mergedModuleName}.jar"
        createJar(jarFilePath, mergedJarsDirPath)
        def modInfoDir = genModuleInfo(project.file(jarFilePath), project.file(tmpJarsDirPath))
        compileModuleInfo(project.file(modInfoDir), project.file(jarFilePath), project.file(tmpModuleInfoDirPath))
        project.copy {
            from jarFilePath
            into jlinkJarsDirPath
        }
        insertModuleInfo(project.file("$jlinkJarsDirPath/${mergedModuleName}.jar"), project.file(tmpModuleInfoDirPath))
    }

    def mergeUnpackedContents(Collection<File> jars, String tmpDirPath) {
        project.logger.info("Merging content into ${tmpDirPath}...")
        project.copy {
            jars.each {from(project.zipTree(it))}
            into(tmpDirPath)
        }
        createManifest(tmpDirPath)
    }

    def compileModuleInfo(File moduleInfoJavaDir, File moduleJar, File targetDir) {
        project.logger.info("Compiling module-info from ${moduleInfoJavaDir}...")
        project.delete(targetDir)
        project.copy {
            from(project.zipTree(moduleJar))
            into(targetDir)
        }
        project.exec {
            commandLine "$javaHome/bin/javac",
                    '-p',
                    "$moduleJar.parentFile$SEP$jlinkJarsDirPath",
                    '-d',
                    targetDir.path,
                    "$moduleInfoJavaDir/module-info.java"
        }
    }

    def insertModuleInfo(File moduleJar, File moduleInfoClassDir) {
        project.logger.info("Inserting module-info into ${moduleJar}...")
        project.exec {
            commandLine "$javaHome/bin/jar",
                    '--update',
                    '--file',
                    moduleJar.path,
                    '-C',
                    moduleInfoClassDir.path,
                    'module-info.class'
        }
    }
}

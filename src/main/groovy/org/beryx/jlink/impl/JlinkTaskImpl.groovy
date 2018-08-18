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

import org.gradle.api.Project

import java.util.jar.JarFile
import java.util.zip.ZipFile

class JlinkTaskImpl {
    static String SEP = File.pathSeparatorChar

    final Project project
    final File imageDir
    final File imageZip
    final Closure beforeZip
    final String moduleName
    final String launcherName
    final String mainClass
    final String mergedModuleName
    final List<String> forceMergedJarPrefixes
    final String javaHome
    final boolean jdepsEnabled

    final String jlinkBasePath
    final String nonModularJarsDirPath
    final String mergedJarsDirPath
    final String tmpMergedModuleDirPath
    final String jlinkJarsDirPath
    final String tmpJarsDirPath
    final String tmpModuleInfoDirPath

    final ModuleInfo mergedModuleInfo

    JlinkTaskImpl(Project project, JlinkTaskData taskData) {
        this.project = project
        this.imageDir = taskData.imageDir
        this.imageZip = taskData.imageZip
        this.beforeZip = taskData.beforeZip
        this.moduleName = taskData.moduleName
        this.launcherName = taskData.launcherName
        this.mainClass = taskData.mainClass
        this.mergedModuleName = taskData.mergedModuleName
        this.forceMergedJarPrefixes = taskData.forceMergedJarPrefixes
        this.javaHome = taskData.javaHome
        this.mergedModuleInfo = taskData.mergedModuleInfo
        this.jdepsEnabled = taskData.jdepsEnabled

        jlinkBasePath = "$project.buildDir/jlinkbase"
        nonModularJarsDirPath = "$jlinkBasePath/nonmodjars"
        mergedJarsDirPath = "$jlinkBasePath/mergedjars"
        tmpMergedModuleDirPath = "$jlinkBasePath/tmpmerged"
        jlinkJarsDirPath = "$jlinkBasePath/jlinkjars"
        tmpJarsDirPath = "$jlinkBasePath/tmpjars"
        tmpModuleInfoDirPath = "$jlinkBasePath/tmpmodinfo"

        project.logger.info("moduleName: $moduleName")
        project.logger.info("launcherName: $launcherName")
        project.logger.info("mainClass: $mainClass")
        project.logger.info("mergedModuleName: $mergedModuleName")
        project.logger.info("forceMergedJarPrefixes: $forceMergedJarPrefixes")
        project.logger.info("javaHome: $javaHome")
        project.logger.info("mergedModuleInfo: $mergedModuleInfo")
    }

    void execute() {
        project.delete(imageDir, imageZip, jlinkBasePath)
        def depMgr = new DependencyManager(project, forceMergedJarPrefixes)
        copyRuntimeJars(depMgr)
        createMergedModule(new File(nonModularJarsDirPath).listFiles() as List)
        createDelegatedModules()

        project.logger.info("Copying modular jars not required by non-modular jars to ${jlinkJarsDirPath}...")
        project.copy {
            into jlinkJarsDirPath
            from (depMgr.modularJars - depMgr.modularJarsRequiredByNonModularJars)
        }
        jlink(project.file(jlinkJarsDirPath))
        if(beforeZip) {
            beforeZip()
        }
        project.ant.zip(destfile: imageZip, duplicate: 'fail') {
            zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/**", excludes: "$imageDir.name/bin/**")
            zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/bin/**", filemode: 755)
        }
    }

    static String getModuleName(File f) {
        def modName = new JarFile(f).getManifest()?.mainAttributes.getValue('Automatic-Module-Name')
        if(modName) return modName
        def s = f.name
        def tokens = s.split('-[0-9]')
        if(tokens.length < 2) return s - '.jar'
        def len = s.length()
        return s.substring(0, len - tokens[-1].length() - 2).replace('-', '.')
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
            if(!entry.name.contains('META-INF') && entry.name.endsWith('.class')) {
                int pos = entry.name.lastIndexOf('/')
                if(pos > 0) {
                    packages << entry.name.substring(0, pos).replace('/', '.')
                }
            }
        }
        def moduleName = getModuleName(jarFile)
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

    File genDelegatedModuleInfo(File jarFile, String targetDirPath) {
        def moduleName = getModuleName(jarFile)
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

    def createDelegatedModules() {
        project.logger.info("Creating delegated modules...")
        project.delete(tmpJarsDirPath)
        new File(nonModularJarsDirPath).eachFile { jarFile ->
            createDelegatedModule(jarFile, tmpJarsDirPath, jlinkJarsDirPath)
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

    def jlink(File modjarsDir) {
        project.delete(imageDir)
        project.delete(imageZip)
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jlinkOutput = {
                return standardOutput.toString()
            }
            commandLine "$javaHome/bin/jlink",
                    '-v',
                    '--module-path',
                    "$javaHome/jmods/$SEP${project.files(modjarsDir).asPath}$SEP${project.jar.archivePath}",
                    '--add-modules', moduleName,
                    '--output', imageDir,
                    '--launcher', "$launcherName=$moduleName/$mainClass"
        }
        if(result.exitValue != 0) {
            project.logger.error(project.ext.jlinkOutput())
        } else {
            project.logger.info(project.ext.jlinkOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }
}

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

import groovy.util.logging.Slf4j
import org.gradle.api.Project

import java.util.jar.JarFile
import java.util.zip.ZipFile

@Slf4j
class JlinkTaskImpl {
    static String SEP = File.pathSeparatorChar

    final Project project
    final File imageDir
    final String moduleName
    final String launcherName
    final String mainClass
    final String mergedModuleName
    final String javaHome

    final String jlinkBasePath
    final String nonModularJarsDirPath
    final String mergedJarsDirPath
    final String tmpMergedModuleDirPath
    final String jlinkJarsDirPath
    final String tmpJarsDirPath
    final String tmpModuleInfoDirPath

    final ModuleInfo mergedModuleInfo

    JlinkTaskImpl(Project project, File imageDir, String moduleName, String launcherName,
                  String mainClass, String mergedModuleName, String javaHome, ModuleInfo mergedModuleInfo) {
        this.project = project
        this.imageDir = imageDir
        this.moduleName = moduleName
        this.launcherName = launcherName
        this.mainClass = mainClass
        this.mergedModuleName = mergedModuleName
        this.javaHome = javaHome
        this.mergedModuleInfo = mergedModuleInfo

        jlinkBasePath = "$project.buildDir/jlinkbase"
        nonModularJarsDirPath = "$jlinkBasePath/nonmodjars"
        mergedJarsDirPath = "$jlinkBasePath/mergedjars"
        tmpMergedModuleDirPath = "$jlinkBasePath/tmpmerged"
        jlinkJarsDirPath = "$jlinkBasePath/jlinkjars"
        tmpJarsDirPath = "$jlinkBasePath/tmpjars"
        tmpModuleInfoDirPath = "$jlinkBasePath/tmpmodinfo"

        log.info("moduleName: $moduleName")
        log.info("launcherName: $launcherName")
        log.info("mainClass: $mainClass")
        log.info("mergedModuleName: $mergedModuleName")
        log.info("javaHome: $javaHome")
        log.info("mergedModuleInfo: $mergedModuleInfo")
    }

    void execute() {
        project.delete(imageDir, jlinkBasePath)
        copyRuntimeJars()
        createMergedModule(new File(nonModularJarsDirPath).listFiles() as List)
        createDelegatedModules()
        jlink(project.file(jlinkJarsDirPath))
    }

    static boolean hasModuleInfo(File f) {
        new ZipFile(f).entries().any {it.name == 'module-info.class'}
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

    static Collection<File> getNonModularJars(File[] allJars) {
        allJars.findAll {!hasModuleInfo(it)}
    }

    File genModuleInfo(File jarFile, File targetDir) {
        log.info("Generating module-info in ${targetDir}...")
        project.delete(targetDir)
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
        def files = targetDir.listFiles()
        return files?.length ? files[0] : genDummyModuleInfo(jarFile, targetDir)
//        return genDummyModuleInfo(jarFile, targetDir)
    }

    File genDummyModuleInfo(File jarFile, File targetDir) {
        def packages = new TreeSet<String>()
        new ZipFile(jarFile).entries().each { entry ->
            if(entry.name.endsWith('.class')) {
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

    def copyRuntimeJars() {
        project.delete(jlinkJarsDirPath, nonModularJarsDirPath)
        log.info("Copying modular jars to ${jlinkJarsDirPath}...")
        project.copy {
            into jlinkJarsDirPath
            from project.configurations.runtimeClasspath.filter {hasModuleInfo(it)}
        }
        log.info("Copying mon-modular jars to ${nonModularJarsDirPath}...")
        project.copy {
            into nonModularJarsDirPath
            from project.configurations.runtimeClasspath.filter {!hasModuleInfo(it)}
        }
    }

    String genDelegatedModuleInfo(File jarFile, String targetDirPath) {
        def moduleName = getModuleName(jarFile)
        def modinfoDir = new File(targetDirPath, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava << """
        open module $moduleName {
            requires transitive $mergedModuleName;
        }
        """.stripIndent()
        modinfoDir.path
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
        def moduleDirPath = genDelegatedModuleInfo(jarFile, tmpDirPath)
        project.delete(tmpModuleInfoDirPath)
        createManifest(tmpModuleInfoDirPath)
        project.exec {
            commandLine "$javaHome/bin/javac",
                    '-p',
                    jlinkJarsDirPath,
                    '-d',
                    tmpModuleInfoDirPath,
                    "$moduleDirPath/module-info.java"
        }
        def targetJarPath = new File(targetDirPath, jarFile.name).path
        createJar(targetJarPath, tmpModuleInfoDirPath)
    }

    def createDelegatedModules() {
        log.info("Creating delegated modules...")
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
        log.info("Creating merged module...")
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
        log.info("Merging content into ${tmpDirPath}...")
        project.copy {
            jars.each {from(project.zipTree(it))}
            into(tmpDirPath)
        }
        createManifest(tmpDirPath)
    }

    def compileModuleInfo(File moduleInfoJavaDir, File moduleJar, File targetDir) {
        log.info("Compiling module-info from ${moduleInfoJavaDir}...")
        project.delete(targetDir)
        project.copy {
            from(project.zipTree(moduleJar))
            into(targetDir)
        }
        project.exec {
            commandLine "$javaHome/bin/javac",
                    '-p',
                    moduleJar.parentFile,
                    '-d',
                    targetDir.path,
                    "$moduleInfoJavaDir/module-info.java"
        }
    }

    def insertModuleInfo(File moduleJar, File moduleInfoClassDir) {
        log.info("Inserting module-info into ${moduleJar}...")
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
        project.exec {
            commandLine "$javaHome/bin/jlink",
                    '-v',
                    '--module-path',
                    "$javaHome/jmods/$SEP${project.files(modjarsDir).asPath}$SEP${project.jar.archivePath}",
                    '--add-modules', moduleName,
                    '--output', imageDir,
                    '--launcher', "$launcherName=$moduleName/$mainClass"
        }
    }
}

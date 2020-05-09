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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.util.JdepsExecutor
import org.beryx.jlink.util.SuggestedMergedModuleInfoBuilder
import org.beryx.jlink.util.Util
import org.beryx.jlink.data.CreateMergedModuleTaskData
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.module.ModuleFinder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class CreateMergedModuleTaskImpl extends BaseTaskImpl<CreateMergedModuleTaskData> {
    private static final Logger LOGGER = Logging.getLogger(CreateMergedModuleTaskImpl.class);

    CreateMergedModuleTaskImpl(Project project, CreateMergedModuleTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        def jarFilePath = "$td.tmpMergedModuleDirPath/$td.mergedModuleJar.name"
        Util.createJar(project, jarFilePath, td.mergedJarsDir)
        def modInfoDir = genModuleInfo(project.file(jarFilePath), project.file(td.tmpJarsDirPath), td.mergedModuleName)
        compileModuleInfo(project.file(modInfoDir), project.file(jarFilePath), project.file(td.tmpModuleInfoDirPath))
        LOGGER.info("Copy from $td.mergedJarsDir into ${td.tmpModuleInfoDirPath}...")
        project.copy {
            from td.mergedJarsDir
            into td.tmpModuleInfoDirPath
        }
        Util.createJar(project, td.mergedModuleJar, project.file(td.tmpModuleInfoDirPath))
    }

    File genModuleInfo(File jarFile, File targetDir) {
        genModuleInfo(jarFile, targetDir, null)
    }

    File genModuleInfo(File jarFile, File targetDir, String moduleName) {
        LOGGER.info("Generating module-info in ${targetDir}...")
        project.delete(targetDir)
        targetDir.mkdirs()
        def moduleInfoFile = genModuleInfoJdeps(jarFile, targetDir)
        if(!moduleInfoFile) {
            moduleInfoFile = genModuleInfoBadass(jarFile, targetDir, moduleName)
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
                def files = targetDir.listFiles({File dir, String name -> name == 'module-info.java'} as FilenameFilter)
                if(files?.length) return files[0]
                LOGGER.warn("jdeps terminated successfully but the module declaration file cannot be found.")
            }
        }
        null
    }

    File genModuleInfoBadass(File jarFile, File targetDir) {
        genModuleInfoBadass(jarFile, targetDir, null)
    }

    File genModuleInfoBadass(File jarFile, File targetDir, String moduleName) {
        def packages = new TreeSet<String>()
        new ZipFile(jarFile).entries().each { ZipEntry entry ->
            def pkgName = Util.getPackage(entry.name)
            if(pkgName) packages << pkgName
        }
        if(!moduleName) {
            moduleName = Util.getFallbackModuleNameFromJarFile(jarFile)
        }
        def modinfoDir = new File(targetDir, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        modInfoJava.delete()
        modInfoJava << "open module $moduleName {\n"
        packages.each {
            modInfoJava << "    exports $it;\n"
        }
        if(td.mergedModuleInfo.shouldUseSuggestions()) {
            def builder = new SuggestedMergedModuleInfoBuilder(
                    project: project,
                    mergedJarsDir: td.mergedJarsDir,
                    javaHome: td.javaHome,
                    forceMergedJarPrefixes: td.forceMergedJarPrefixes,
                    extraDependenciesPrefixes: td.extraDependenciesPrefixes,
                    configuration: td.configuration,
                    constraints: td.mergedModuleInfo.additiveConstraints
            )
            modInfoJava << builder.moduleInfo.toString(4)
        }
        if(td.mergedModuleInfo.enabled) {
            modInfoJava << td.mergedModuleInfo.toString(4)
        }
        modInfoJava << '\n}\n'
        modinfoDir
    }

    @CompileDynamic
    def compileModuleInfo(File moduleInfoJavaDir, File moduleJar, File targetDir) {
        LOGGER.info("Compiling module-info from ${moduleInfoJavaDir}...")
        project.delete(targetDir)
        project.copy {
            from(project.zipTree(moduleJar))
            into(targetDir)
        }

        project.exec {
            commandLine = [
                "$td.javaHome/bin/javac",
                *versionOpts,
                '-p',
                "$td.mergedJarsDir$SEP$td.jlinkJarsDirPath",
                '-d',
                targetDir.path,
                "$moduleInfoJavaDir/module-info.java"
            ]
        }
    }

    private List<String> getVersionOpts() {
        def version = td.mergedModuleInfo.version
        if(!version) {
            def archiveFile = Util.getArchiveFile(project)
            def moduleRef = ModuleFinder.of(archiveFile.toPath()).findAll().find()
            if(moduleRef) {
                moduleRef.descriptor().version().ifPresent{v -> version = v.toString()}
            }
            if(!version) {
                def projectVersion = project.version as String
                if(projectVersion && projectVersion != Project.DEFAULT_VERSION) {
                    version = projectVersion
                }
            }
        }
        (!version || version == Project.DEFAULT_VERSION) ? [] : [ '--module-version', version ]
    }
}

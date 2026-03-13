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
package org.beryx.jlink


import groovy.transform.CompileStatic
import org.beryx.jlink.data.CreateMergedModuleTaskData
import org.beryx.jlink.data.DependencyData
import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.data.ModuleInfo
import org.beryx.jlink.impl.CreateMergedModuleTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CompileStatic
abstract class CreateMergedModuleTask extends BaseTask {
    @Input
    List<String> getForceMergedJarPrefixes() {
        extension.forceMergedJarPrefixes.get()
    }

    @Input
    List<String> getExtraDependenciesPrefixes() {
        extension.extraDependenciesPrefixes.get()
    }

    @Input
    String getMergedModuleName() {
        extension.mergedModuleName.get()
    }

    @Input
    String getMergedModuleJarName() {
        String jarName = extension.mergedModuleJarName.get()
        if(jarName.endsWith('.jar')) jarName = jarName[0 .. -5]
        jarName
    }

    @InputDirectory
    Directory getMergedJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getMergedJarsDirPath(jlinkBasePath))
    }

    @Input
    ModuleInfo getMergedModuleInfo() {
        extension.mergedModuleInfo.get()
    }

    @Input
    JdepsUsage getUseJdeps() {
        extension.useJdeps.get()
    }

    @Input
    String getJavaHome() {
        javaHomeOrDefault
    }

    @Input
    String getConfiguration() {
        extension.configuration.get()
    }

    @OutputFile
    File getMergedModuleJar() {
        String jarFileName = mergedModuleJarName
        if(mergedModuleInfo.version != Project.DEFAULT_VERSION) {
            jarFileName += "-$mergedModuleInfo.version"
        }
        jarFileName += ".jar"
        new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath), jarFileName)
    }

    @Internal
    abstract Property<DependencyData> getDependencyDataProperty()

    @Classpath
    abstract ConfigurableFileCollection getClasspathFiles()

    CreateMergedModuleTask() {
        dependsOn(JlinkPlugin.TASK_NAME_PREPARE_MERGED_JARS_DIR)
        description = 'Unpacks all non-modularized jars into a single directory'
        project.getGradle().projectsEvaluated {
            def configName = extension.configuration.get()
            def config = project.configurations.getByName(configName)
            dependencyDataProperty.set(project.provider { DependencyData.from(config) })
            classpathFiles.from(config)
        }
    }

    @TaskAction
    void createMergedModuleAction() {
        def taskData = new CreateMergedModuleTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes
        taskData.extraDependenciesPrefixes = extraDependenciesPrefixes
        taskData.mergedModuleName = mergedModuleName
        taskData.mergedModuleInfo = mergedModuleInfo
        taskData.useJdeps = useJdeps
        taskData.mergedModuleJar = mergedModuleJar
        taskData.mergedJarsDir = mergedJarsDir.asFile
        taskData.javaHome = javaHome
        taskData.dependencyData = dependencyDataProperty.get()
        taskData.archiveFile = Util.getArchiveFile(project)

        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpMergedModuleDirPath = PathUtil.getTmpMergedModuleDirPath(taskData.jlinkBasePath)
        taskData.tmpModuleInfoDirPath = PathUtil.getTmpModuleInfoDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)

        def taskImpl = new CreateMergedModuleTaskImpl(fileSystemOperations, archiveOperations, execOperations, project.version.toString(), taskData)
        taskImpl.execute()
    }
}

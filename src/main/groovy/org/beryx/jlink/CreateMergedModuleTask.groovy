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
import groovy.transform.CompileDynamic
import org.beryx.jlink.data.CreateMergedModuleTaskData
import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.data.ModuleInfo
import org.beryx.jlink.impl.CreateMergedModuleTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CreateMergedModuleTask extends BaseTask {
    @Input
    ListProperty<String> forceMergedJarPrefixes

    @Input
    ListProperty<String> extraDependenciesPrefixes

    @Input
    Property<String> mergedModuleName

    @InputDirectory
    DirectoryProperty mergedJarsDir

    @Input
    Property<ModuleInfo> mergedModuleInfo

    @Input
    Property<JdepsUsage> useJdeps

    @Input
    Property<String> javaHome

    @OutputFile
    @CompileDynamic
    File getMergedModuleJar() {
        new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath.get()), "${Util.getArchiveBaseName(project)}.merged.module.jar")
    }

    CreateMergedModuleTask() {
        dependsOn(JlinkPlugin.TASK_NAME_PREPARE_MERGED_JARS_DIR)
        description = 'Unpacks all non-modularized jars into a single directory'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        forceMergedJarPrefixes = extension.forceMergedJarPrefixes
        extraDependenciesPrefixes = extension.extraDependenciesPrefixes
        mergedModuleName = extension.mergedModuleName
        mergedModuleInfo = extension.mergedModuleInfo
        useJdeps = extension.useJdeps
        javaHome = extension.javaHome

        mergedJarsDir = Util.createDirectoryProperty(project)
        mergedJarsDir.set(project.layout.buildDirectory.dir(PathUtil.getMergedJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void createMergedModuleAction() {
        def taskData = new CreateMergedModuleTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.forceMergedJarPrefixes = (List<String>)forceMergedJarPrefixes.get()
        taskData.extraDependenciesPrefixes = (List<String>)extraDependenciesPrefixes.get()
        taskData.mergedModuleName = mergedModuleName.get()
        taskData.mergedModuleInfo = mergedModuleInfo.get()
        taskData.useJdeps = useJdeps.get()
        taskData.mergedModuleJar = mergedModuleJar
        taskData.mergedJarsDir = mergedJarsDir.get().asFile
        taskData.javaHome = javaHome.get()

        taskData.nonModularJarsDirPath = PathUtil.getNonModularJarsDirPath(taskData.jlinkBasePath)
        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpMergedModuleDirPath = PathUtil.getTmpMergedModuleDirPath(taskData.jlinkBasePath)
        taskData.tmpModuleInfoDirPath = PathUtil.getTmpModuleInfoDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)

        def taskImpl = new CreateMergedModuleTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

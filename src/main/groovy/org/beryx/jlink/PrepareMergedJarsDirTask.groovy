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
import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.data.PrepareMergedJarsDirTaskData
import org.beryx.jlink.impl.PrepareMergedJarsDirTaskImpl
import org.beryx.jlink.util.JavaVersion
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class PrepareMergedJarsDirTask extends BaseTask {
    @Input
    ListProperty<String> forceMergedJarPrefixes

    @Input
    ListProperty<String> extraDependenciesPrefixes

    @Input
    Property<String> javaHome

    @Optional @Input
    Property<Integer> jvmVersion

    @OutputDirectory
    DirectoryProperty mergedJarsDir

    PrepareMergedJarsDirTask() {
        dependsOn('jar')
        description = 'Merges all non-modularized jars into a single module'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        forceMergedJarPrefixes = extension.forceMergedJarPrefixes
        extraDependenciesPrefixes = extension.extraDependenciesPrefixes
        javaHome = extension.javaHome
        jvmVersion = extension.jvmVersion

        mergedJarsDir = Util.createDirectoryProperty(project)
        mergedJarsDir.set(project.layout.buildDirectory.dir(PathUtil.getMergedJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void createMergedModuleAction() {
        def taskData = new PrepareMergedJarsDirTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes.get()
        taskData.extraDependenciesPrefixes = extraDependenciesPrefixes.get()
        taskData.mergedJarsDir = mergedJarsDir.get().asFile
        taskData.javaHome = javaHome.get()
        if(jvmVersion.present && jvmVersion.get()) {
            taskData.jvmVersion = jvmVersion.get()
        } else {
            taskData.jvmVersion = JavaVersion.get(taskData.javaHome)
        }

        taskData.nonModularJarsDirPath = PathUtil.getNonModularJarsDirPath(taskData.jlinkBasePath)
        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)

        def taskImpl = new PrepareMergedJarsDirTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

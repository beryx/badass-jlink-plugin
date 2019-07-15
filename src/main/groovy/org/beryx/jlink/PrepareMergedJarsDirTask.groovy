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
import org.beryx.jlink.data.PrepareMergedJarsDirTaskData
import org.beryx.jlink.impl.PrepareMergedJarsDirTaskImpl
import org.beryx.jlink.util.JavaVersion
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar

@CompileStatic
class PrepareMergedJarsDirTask extends BaseTask {
    @Input
    List<String> getForceMergedJarPrefixes() {
        extension.forceMergedJarPrefixes.get()
    }

    @Input
    List<String> getExtraDependenciesPrefixes() {
        extension.extraDependenciesPrefixes.get()
    }

    @Input
    String getJavaHome() {
        extension.javaHome.get()
    }

    @Optional @Input
    Integer getJvmVersion() {
        extension.jvmVersion.getOrElse(null)
    }

    @OutputDirectory
    Directory getMergedJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getMergedJarsDirPath(jlinkBasePath))
    }

    PrepareMergedJarsDirTask() {
        def jarTasks = project.rootProject.getTasksByName('jar', true).toArray()
        dependsOn(jarTasks)
        description = 'Merges all non-modularized jars into a single module'
    }

    @TaskAction
    void createMergedModuleAction() {
        def taskData = new PrepareMergedJarsDirTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes
        taskData.extraDependenciesPrefixes = extraDependenciesPrefixes
        taskData.mergedJarsDir = mergedJarsDir.asFile
        taskData.javaHome = javaHome
        taskData.jvmVersion = jvmVersion ?: JavaVersion.get(taskData.javaHome)

        taskData.nonModularJarsDirPath = PathUtil.getNonModularJarsDirPath(taskData.jlinkBasePath)
        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)

        def taskImpl = new PrepareMergedJarsDirTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @InputFile
    public File getArchivePath() {
        Jar jarTask = (Jar)project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
        jarTask.archivePath
    }
}

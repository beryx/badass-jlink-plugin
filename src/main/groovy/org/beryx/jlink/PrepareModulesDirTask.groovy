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
import org.beryx.jlink.data.PrepareModulesDirTaskData
import org.beryx.jlink.impl.PrepareModulesDirTaskImpl
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class PrepareModulesDirTask extends BaseTask {
    @Input
    String getModuleName() {
        extension.moduleName.get()
    }

    @Input
    String getMergedModuleName() {
        extension.mergedModuleName.get()
    }

    @Input
    String getJavaHome() {
        extension.javaHome.get()
    }

    @Input
    String getConfiguration() {
        extension.configuration.get()
    }

    @Input
    List<String> getForceMergedJarPrefixes() {
        extension.forceMergedJarPrefixes.get()
    }

    @Input
    List<String> getExtraDependenciesPrefixes() {
        extension.extraDependenciesPrefixes.get()
    }

    @InputDirectory
    Directory getDelegatingModulesDir() {
        project.layout.projectDirectory.dir(PathUtil.getDelegatingModulesDirPath(jlinkBasePath))
    }

    @OutputDirectory
    Directory getJlinkJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getJlinkJarsDirPath(jlinkBasePath))
    }

    PrepareModulesDirTask() {
        dependsOn(JlinkPlugin.TASK_NAME_CREATE_DELEGATING_MODULES)
        description = 'Prepares the directory containing modules required by the application'
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new PrepareModulesDirTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.moduleName = moduleName
        taskData.mergedModuleName = mergedModuleName
        taskData.javaHome = javaHome
        taskData.configuration = project.configurations.getByName(configuration)
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes
        taskData.extraDependenciesPrefixes = extraDependenciesPrefixes
        taskData.delegatingModulesDir = delegatingModulesDir.asFile
        taskData.jlinkJarsDir = jlinkJarsDir.asFile
        taskData.tmpModuleInfoDirPath = PathUtil.getTmpModuleInfoDirPath(taskData.jlinkBasePath)

        def taskImpl = new PrepareModulesDirTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

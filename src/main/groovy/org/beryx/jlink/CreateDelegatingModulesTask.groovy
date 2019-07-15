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
import org.beryx.jlink.data.CreateDelegatingModulesTaskData
import org.beryx.jlink.impl.CreateDelegatingModulesTaskImpl
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class CreateDelegatingModulesTask extends BaseTask {
    CreateDelegatingModulesTask() {
        dependsOn(JlinkPlugin.TASK_NAME_CREATE_MERGED_MODULE)
        description = 'Creates delegating modules for the jars that have been merged into a single module'
    }

    @Input
    String getMergedModuleName() {
        extension.mergedModuleName.get()
    }

    @Input
    String getJavaHome() {
        extension.javaHome.get()
    }

    @InputDirectory
    Directory getNonModularJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getNonModularJarsDirPath(jlinkBasePath))
    }

    @OutputDirectory
    Directory getDelegatingModulesDir() {
        project.layout.projectDirectory.dir(PathUtil.getDelegatingModulesDirPath(jlinkBasePath))
    }


    @TaskAction
    void createDelegatingModulesAction() {
        def taskData = new CreateDelegatingModulesTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.mergedModuleName = mergedModuleName
        taskData.javaHome = javaHome
        taskData.nonModularJarsDir = nonModularJarsDir.asFile
        taskData.delegatingModulesDir = delegatingModulesDir.asFile

        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpModuleInfoDirPath = PathUtil.getTmpModuleInfoDirPath(taskData.jlinkBasePath)

        def taskImpl = new CreateDelegatingModulesTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

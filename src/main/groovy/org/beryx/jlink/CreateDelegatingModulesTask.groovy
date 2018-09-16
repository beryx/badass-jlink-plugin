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

import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.impl.CreateDelegatingModulesTaskImpl
import org.beryx.jlink.data.CreateDelegatingModulesTaskData
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class CreateDelegatingModulesTask extends BaseTask {
    @Input
    Property<String> mergedModuleName

    @Input
    Property<String> javaHome

    @InputDirectory
    DirectoryProperty nonModularJarsDir

    @OutputDirectory
    DirectoryProperty delegatingModulesDir

    CreateDelegatingModulesTask() {
        dependsOn(JlinkPlugin.TASK_NAME_CREATE_MERGED_MODULE)
        description = 'Creates delegating modules for the jars that have been merged into a single module'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        mergedModuleName = extension.mergedModuleName
        javaHome = extension.javaHome

        nonModularJarsDir = project.layout.directoryProperty()
        nonModularJarsDir.set(new File(PathUtil.getNonModularJarsDirPath(jlinkBasePath.get())))

        delegatingModulesDir = project.layout.directoryProperty()
        delegatingModulesDir.set(new File(PathUtil.getDelegatingModulesDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void createDelegatingModulesAction() {
        def taskData = new CreateDelegatingModulesTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.mergedModuleName = mergedModuleName.get()
        taskData.javaHome = javaHome.get()
        taskData.nonModularJarsDir = nonModularJarsDir.get().asFile
        taskData.delegatingModulesDir = delegatingModulesDir.get().asFile

        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpModuleInfoDirPath = PathUtil.getTmpModuleInfoDirPath(taskData.jlinkBasePath)

        def taskImpl = new CreateDelegatingModulesTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

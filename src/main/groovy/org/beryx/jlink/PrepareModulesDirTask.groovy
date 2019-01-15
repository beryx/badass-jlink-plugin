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
import org.beryx.jlink.data.PrepareModulesDirTaskData
import org.beryx.jlink.impl.PrepareModulesDirTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class PrepareModulesDirTask extends BaseTask {
    @Input
    ListProperty<String> forceMergedJarPrefixes

    @Input
    ListProperty<String> extraDependenciesPrefixes

    @InputDirectory
    DirectoryProperty delegatingModulesDir

    @OutputDirectory
    DirectoryProperty jlinkJarsDir

    PrepareModulesDirTask() {
        dependsOn(JlinkPlugin.TASK_NAME_CREATE_DELEGATING_MODULES)
        description = 'Prepares the directory containing modules required by the application'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        forceMergedJarPrefixes = extension.forceMergedJarPrefixes
        extraDependenciesPrefixes = extension.extraDependenciesPrefixes

        delegatingModulesDir = Util.createDirectoryProperty(project)
        delegatingModulesDir.set(new File(PathUtil.getDelegatingModulesDirPath(jlinkBasePath.get())))

        jlinkJarsDir = Util.createDirectoryProperty(project)
        jlinkJarsDir.set(new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new PrepareModulesDirTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes.get()
        taskData.extraDependenciesPrefixes = extraDependenciesPrefixes.get()
        taskData.delegatingModulesDir = delegatingModulesDir.get().asFile
        taskData.jlinkJarsDir = jlinkJarsDir.get().asFile

        def taskImpl = new PrepareModulesDirTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

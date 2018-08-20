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

import org.gradle.api.Plugin
import org.gradle.api.Project

class JlinkPlugin implements Plugin<Project> {
    final static TASK_NAME_CREATE_MERGED_MODULE = 'createMergedModule'
    final static TASK_NAME_CREATE_DELEGATED_MODULES = 'createDelegatedModules'
    final static TASK_NAME_PREPARE_MODULES_DIR = 'ptepareModulesDir'
    final static TASK_NAME_JLINK = 'jlink'
    final static TASK_NAME_JLINK_ZIP = 'jlinkZip'

    @Override
    void apply(Project project) {
        project.getPluginManager().apply('application');
        def extension = project.extensions.create('jlink', JlinkPluginExtension, project)

        project.getTasks().create(TASK_NAME_CREATE_MERGED_MODULE, CreateMergedModuleTask, { CreateMergedModuleTask task ->
            task.mergedModuleName = extension.mergedModuleName
            task.forceMergedJarPrefixes = extension.forceMergedJarPrefixes
            task.javaHome = extension.javaHome
            task.mergedModuleInfo = extension.mergedModuleInfo
            task.jdepsEnabled = extension.jdepsEnabled
        })

        project.getTasks().create(TASK_NAME_CREATE_DELEGATED_MODULES, CreateDelegatedModulesTask, { CreateDelegatedModulesTask task ->
            task.mergedModuleName = extension.mergedModuleName
            task.javaHome = extension.javaHome
        })

        project.getTasks().create(TASK_NAME_PREPARE_MODULES_DIR, PrepareModulesDirTask, { PrepareModulesDirTask task ->
            task.forceMergedJarPrefixes = extension.forceMergedJarPrefixes
        })

        project.getTasks().create(TASK_NAME_JLINK, JlinkTask, { JlinkTask task ->
            task.launcherName = extension.launcherName
            task.mainClass = extension.mainClass
            task.moduleName = extension.moduleName
            task.options = extension.options
            task.javaHome = extension.javaHome
            task.imageDir = extension.imageDir
        })

        project.getTasks().create(TASK_NAME_JLINK_ZIP, JlinkZipTask, { JlinkZipTask task ->
            task.imageDir = extension.imageDir
            task.imageZip = extension.imageZip
        })
    }
}

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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

@CompileStatic
class JlinkPlugin implements Plugin<Project> {
    final static String EXTENSION_NAME = 'jlink'
    final static String TASK_NAME_PREPARE_MERGED_JARS_DIR = 'prepareMergedJarsDir'
    final static String TASK_NAME_CREATE_MERGED_MODULE = 'createMergedModule'
    final static String TASK_NAME_CREATE_DELEGATING_MODULES = 'createDelegatingModules'
    final static String TASK_NAME_PREPARE_MODULES_DIR = 'prepareModulesDir'
    final static String TASK_NAME_JLINK = 'jlink'
    final static String TASK_NAME_JLINK_ZIP = 'jlinkZip'
    final static String TASK_NAME_SUGGEST_MERGED_MODULE_INFO = 'suggestMergedModuleInfo'

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            throw new GradleException("This version of the plugin requires Gradle 5 or newer.\n" +
                "Upgrade to Gradle 5 or use a version with the '-gradle4' suffix.")
        }
        project.getPluginManager().apply('application');
        def extension = project.extensions.create(EXTENSION_NAME, JlinkPluginExtension, project)
        project.tasks.create(TASK_NAME_PREPARE_MERGED_JARS_DIR, PrepareMergedJarsDirTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_CREATE_MERGED_MODULE, CreateMergedModuleTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_CREATE_DELEGATING_MODULES, CreateDelegatingModulesTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_PREPARE_MODULES_DIR, PrepareModulesDirTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_JLINK, JlinkTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_JLINK_ZIP, JlinkZipTask, { it.init(extension) })
        project.tasks.create(TASK_NAME_SUGGEST_MERGED_MODULE_INFO, SuggestMergedModuleInfoTask, { it.init(extension) })
    }
}

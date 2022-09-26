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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

class JlinkPlugin implements Plugin<Project> {
    final static String EXTENSION_NAME = 'jlink'
    final static String TASK_NAME_PREPARE_MERGED_JARS_DIR = 'prepareMergedJarsDir'
    final static String TASK_NAME_CREATE_MERGED_MODULE = 'createMergedModule'
    final static String TASK_NAME_CREATE_DELEGATING_MODULES = 'createDelegatingModules'
    final static String TASK_NAME_PREPARE_MODULES_DIR = 'prepareModulesDir'
    final static String TASK_NAME_JLINK = 'jlink'
    final static String TASK_NAME_JLINK_ZIP = 'jlinkZip'
    final static String TASK_NAME_JPACKAGE_IMAGE = 'jpackageImage'
    final static String TASK_NAME_JPACKAGE = 'jpackage'
    final static String TASK_NAME_SUGGEST_MERGED_MODULE_INFO = 'suggestMergedModuleInfo'

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('4.0')) {
            throw new GradleException("This plugin requires Gradle 4.8 or newer.")
        }
        project.getPluginManager().apply('application');
        project.extensions.create(EXTENSION_NAME, JlinkPluginExtension, project)
        project.tasks.create(TASK_NAME_PREPARE_MERGED_JARS_DIR, PrepareMergedJarsDirTask)
        project.tasks.create(TASK_NAME_CREATE_MERGED_MODULE, CreateMergedModuleTask)
        project.tasks.create(TASK_NAME_CREATE_DELEGATING_MODULES, CreateDelegatingModulesTask)
        project.tasks.create(TASK_NAME_PREPARE_MODULES_DIR, PrepareModulesDirTask)
        project.tasks.create(TASK_NAME_JLINK, JlinkTask)
        project.tasks.create(TASK_NAME_JLINK_ZIP, JlinkZipTask)
        project.tasks.create(TASK_NAME_JPACKAGE_IMAGE, JPackageImageTask)
        project.tasks.create(TASK_NAME_JPACKAGE, JPackageTask)
        project.tasks.create(TASK_NAME_SUGGEST_MERGED_MODULE_INFO, SuggestMergedModuleInfoTask)
    }
}

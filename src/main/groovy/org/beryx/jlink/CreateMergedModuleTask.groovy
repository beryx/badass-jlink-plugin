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

import org.beryx.jlink.impl.CreateMergedModuleTaskImpl
import org.beryx.jlink.impl.ModuleInfo
import org.beryx.jlink.taskdata.CreateMergedModuleTaskData
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class CreateMergedModuleTask extends DefaultTask {
    @Input
    Property<String> mergedModuleName

    @Input
    Property<List<String>> forceMergedJarPrefixes

    @Input
    Property<String> javaHome

    @Input
    Property<ModuleInfo> mergedModuleInfo

    @Input
    Property<String> jdepsEnabled

    CreateMergedModuleTask() {
        dependsOn('jar')
        group = 'build'
        description = 'Merges all non-modularized jars into a single module'
    }

    @TaskAction
    void createMergedModuleAction() {
        def taskData = new CreateMergedModuleTaskData()
        taskData.mergedModuleName = mergedModuleName.get() ?: Util.getDefaultMergedModuleName(project)
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes.get()
        taskData.javaHome = javaHome.get() ?: System.getenv('JAVA_HOME')
        taskData.mergedModuleInfo = mergedModuleInfo.get()
        taskData.jdepsEnabled = jdepsEnabled.get()
        def taskImpl = new CreateMergedModuleTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

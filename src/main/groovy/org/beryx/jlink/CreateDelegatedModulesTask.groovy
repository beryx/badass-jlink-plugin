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

import org.beryx.jlink.impl.CreateDelegatedModulesTaskImpl
import org.beryx.jlink.taskdata.CreateDelegatedModulesTaskData
import org.beryx.jlink.util.Util
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class CreateDelegatedModulesTask extends DefaultTask {
    @Input
    Property<String> mergedModuleName

    @Input
    Property<String> javaHome

    CreateDelegatedModulesTask() {
        dependsOn(JlinkPlugin.TASK_NAME_CREATE_MERGED_MODULE)
        group = 'build'
        description = 'Creates delegated modules for the jars that have been merged into a single module'
    }

    @TaskAction
    void createDelegatedModulesAction() {
        def taskData = new CreateDelegatedModulesTaskData()
        taskData.mergedModuleName = mergedModuleName.get() ?: Util.getDefaultMergedModuleName(project)
        taskData.javaHome = javaHome.get() ?: System.getenv('JAVA_HOME')
        def taskImpl = new CreateDelegatedModulesTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

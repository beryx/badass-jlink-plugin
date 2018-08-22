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
import org.beryx.jlink.impl.JlinkZipTaskImpl
import org.beryx.jlink.data.JlinkZipTaskData
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class JlinkZipTask extends BaseTask {
    @OutputDirectory
    DirectoryProperty imageDir

    @OutputFile
    RegularFileProperty imageZip

    JlinkZipTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates a zip of the modular runtime image'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        imageDir = extension.imageDir
        imageZip = extension.imageZip
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkZipTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.imageDir = imageDir.get().asFile
        taskData.imageZip = imageZip.get().asFile
        def taskImpl = new JlinkZipTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

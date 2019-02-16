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
import org.beryx.jlink.data.JlinkZipTaskData
import org.beryx.jlink.data.LauncherData
import org.beryx.jlink.data.TargetPlatform
import org.beryx.jlink.impl.JlinkZipTaskImpl
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

@CompileStatic
class JlinkZipTask extends BaseTask {
    @Input
    Provider<Map<String, TargetPlatform>> targetPlatforms

    @Input
    Property<LauncherData> launcherData

    @Internal
    Property<String> imageName

    @Internal
    DirectoryProperty imageDir

    @Internal
    RegularFileProperty imageZip

    JlinkZipTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates a zip of the modular runtime image'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        targetPlatforms = extension.targetPlatforms
        launcherData = extension.launcherData
        imageName = extension.imageName
        imageDir = extension.imageDir
        imageZip = extension.imageZip
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkZipTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.targetPlatforms = targetPlatforms.get()
        taskData.launcherData = launcherData.get()
        taskData.imageDir = getImageDirAsFile()
        taskData.imageZip = getImageZipAsFile()
        def taskImpl = new JlinkZipTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    File getImageDirAsFile() {
        imageName.get() ? imageDirFromName : imageDir.get().asFile
    }

    @OutputFile @PathSensitive(PathSensitivity.NONE)
    File getImageZipAsFile() {
        imageName.get() ? imageZipFromName : imageZip.get().asFile
    }

    @Internal
    File getImageDirFromName() {
        project.file("$project.buildDir/${imageName.get()}")
    }

    @Internal
    File getImageZipFromName() {
        project.file("$project.buildDir/${imageName.get()}.zip")
    }
}

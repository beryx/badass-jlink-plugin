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
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.*

@CompileStatic
class JlinkZipTask extends BaseTask {
    @Input
    Map<String, TargetPlatform> getTargetPlatforms() {
        extension.getTargetPlatforms().get()
    }

    @Input
    LauncherData getLauncherData() {
        extension.launcherData.get()
    }

    @Internal
    String getImageName() {
        extension.imageName.get()
    }

    @Internal
    Directory getImageDir() {
        extension.imageDir.get()
    }

    @Internal
    RegularFile getImageZip() {
        extension.imageZip.get()
    }

    JlinkZipTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates a zip of the modular runtime image'
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkZipTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.targetPlatforms = targetPlatforms
        taskData.launcherData = launcherData
        taskData.imageDir = getImageDirAsFile()
        taskData.imageZip = getImageZipAsFile()
        def taskImpl = new JlinkZipTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    File getImageDirAsFile() {
        imageName ? imageDirFromName : imageDir.asFile
    }

    @OutputFile @PathSensitive(PathSensitivity.NONE)
    File getImageZipAsFile() {
        imageName ? imageZipFromName : imageZip.asFile
    }

    @Internal
    File getImageDirFromName() {
        project.file("$project.buildDir/$imageName")
    }

    @Internal
    File getImageZipFromName() {
        project.file("$project.buildDir/${imageName}.zip")
    }
}

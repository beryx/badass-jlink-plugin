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
import org.beryx.jlink.data.JPackageData
import org.beryx.jlink.data.JPackageTaskData
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

@CompileStatic
abstract class AbstractJPackageTask extends BaseTask {
    @Input
    String getModuleName() {
        extension.moduleName.get()
    }

    @Input
    String getMainClass() {
        extension.mainClass.get()
    }

    @InputDirectory
    Directory getJlinkJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getJlinkJarsDirPath(jlinkBasePath))
    }

    @Input
    String getImageName() {
        extension.imageName.get()
    }

    @InputDirectory
    File getImageInputDir() {
        jlinkTask.imageDirAsFile
    }

    @Nested
    JPackageData getJpackageData() {
        extension.jpackageData.get()
    }

    @Internal
    protected JlinkTask getJlinkTask() {
        (JlinkTask) project.tasks.getByName(JlinkPlugin.TASK_NAME_JLINK)
    }

    protected JPackageTaskData createTaskData() {
        def taskData = new JPackageTaskData()
        taskData.defaultJvmArgs = org.beryx.jlink.util.Util.getDefaultJvmArgs(project) ?: []
        taskData.defaultArgs = org.beryx.jlink.util.Util.getDefaultArgs(project) ?: []
        taskData.projectVersion = project.version.toString()
        taskData.projectArchiveFile = project.tasks.getByName('jar').outputs.files.singleFile
        taskData.jlinkBasePath = jlinkBasePath
        taskData.imageDir = imageInputDir
        taskData.moduleName = moduleName
        taskData.jpackageData = jpackageData
        taskData.mainClass = mainClass ?: defaultMainClass
        taskData.configureRuntimeImageDir(jlinkTask)
        taskData
    }
}

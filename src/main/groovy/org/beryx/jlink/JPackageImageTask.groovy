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
import org.beryx.jlink.impl.JPackageImageTaskImpl
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*

@CompileStatic
class JPackageImageTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTask.class)

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
        extension.imageName
    }

    @OutputDirectory
    Directory getImageDir() {
        extension.imageDir.get()
    }

    @Nested
    JPackageData getJpackageData() {
        extension.jpackageData.get()
    }

    JPackageImageTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates an installable image using the jpackage tool'
    }

    @TaskAction
    void jpackageTaskAction() {
        def taskData = new JPackageTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.imageDir = imageName ? imageDirFromName : imageDir.asFile
        taskData.moduleName = moduleName
        taskData.jpackageData = jpackageData
        taskData.mainClass = mainClass ?: defaultMainClass
        taskData.jlinkJarsDir = jlinkJarsDir.asFile

        def jlinkTask = (JlinkTask) project.tasks.getByName(JlinkPlugin.TASK_NAME_JLINK)
        taskData.configureRuntimeImageDir(jlinkTask)

        def taskImpl = new JPackageImageTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @Internal
    File getImageDirFromName() {
        project.file("$project.buildDir/${imageName}")
    }
}

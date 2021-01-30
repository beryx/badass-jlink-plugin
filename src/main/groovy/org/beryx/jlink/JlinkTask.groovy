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
import org.beryx.jlink.data.*
import org.beryx.jlink.impl.JlinkTaskImpl
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*

@CompileStatic
class JlinkTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(JlinkTask.class);

    @Input
    String getModuleName() {
        extension.moduleName.get()
    }

    @Input
    LauncherData getLauncherData() {
        extension.launcherData.get()
    }

    @Input
    List<SecondaryLauncherData> getSecondaryLaunchers() {
        extension.secondaryLaunchers.get()
    }

    @Input
    CustomImageData getCustomImageData() {
        extension.customImageData.get()
    }

    @Input
    String getMainClass() {
        extension.mainClass.get()
    }

    @Input
    String getConfiguration() {
        extension.configuration.get()
    }

    @Input
    List<String> getOptions() {
        extension.options.get()
    }

    @Input
    List<String> getExtraModulePaths() {
        extension.extraModulePaths.get()
    }

    @Input
    String getJavaHome() {
        javaHomeOrDefault
    }

    @Input
    Map<String, TargetPlatform> getTargetPlatforms() {
        extension.targetPlatforms.get()
    }

    @InputDirectory
    Directory getJlinkJarsDir() {
        project.layout.projectDirectory.dir(PathUtil.getJlinkJarsDirPath(jlinkBasePath))
    }

    @Internal
    String getImageName() {
        extension.imageName.get()
    }

    @Internal
    Directory getImageDir() {
        extension.imageDir.get()
    }

    @Input
    CdsData getCdsData() {
        extension.cdsData.get()
    }

    JlinkTask() {
        dependsOn(JlinkPlugin.TASK_NAME_PREPARE_MODULES_DIR)
        description = 'Creates a modular runtime image with jlink'
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkTaskData()
        taskData.jlinkBasePath = jlinkBasePath
        taskData.imageDir = getImageDirAsFile()
        taskData.moduleName = moduleName
        taskData.launcherData = launcherData
        taskData.secondaryLaunchers = secondaryLaunchers
        taskData.customImageData = customImageData
        taskData.mainClass = mainClass ?: defaultMainClass
        taskData.configuration = project.configurations.getByName(configuration)
        taskData.options = options
        taskData.extraModulePaths = extraModulePaths
        taskData.javaHome = javaHome
        taskData.targetPlatforms = targetPlatforms
        taskData.jlinkJarsDir = jlinkJarsDir.asFile
        taskData.cdsData = cdsData

        def taskImpl = new JlinkTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @OutputDirectory
    File getImageDirAsFile() {
        imageName ? imageDirFromName : imageDir.asFile
    }

    @Internal
    File getImageDirFromName() {
        project.file("$project.buildDir/$imageName")
    }
}

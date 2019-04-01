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
import org.beryx.jlink.data.JlinkTaskData
import org.beryx.jlink.data.LauncherData
import org.beryx.jlink.data.TargetPlatform
import org.beryx.jlink.impl.JlinkTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CompileStatic
class JlinkTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(JlinkTask.class);

    @Input
    Property<String> moduleName

    @Input
    Property<LauncherData> launcherData

    @Input
    Property<String> mainClass

    @Input
    ListProperty<String> options

    @Input
    ListProperty<String> extraModulePaths

    @Input
    Property<String> javaHome

    @Input
    Provider<Map<String, TargetPlatform>> targetPlatforms

    @InputDirectory
    DirectoryProperty jlinkJarsDir

    @Internal
    Property<String> imageName

    @Internal
    DirectoryProperty imageDir

    JlinkTask() {
        dependsOn(JlinkPlugin.TASK_NAME_PREPARE_MODULES_DIR)
        description = 'Creates a modular runtime image with jlink'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        launcherData = extension.launcherData
        mainClass = extension.mainClass
        moduleName = extension.moduleName
        options = extension.options
        extraModulePaths = extension.extraModulePaths
        javaHome = extension.javaHome
        targetPlatforms = extension.targetPlatforms
        imageName = extension.imageName
        imageDir = extension.imageDir

        jlinkJarsDir = Util.createDirectoryProperty(project)
        jlinkJarsDir.set(new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.imageDir = getImageDirAsFile()
        taskData.moduleName = moduleName.get()
        taskData.launcherData = launcherData.get()
        taskData.mainClass = mainClass.get() ?: defaultMainClass
        taskData.options = options.get()
        taskData.extraModulePaths = extraModulePaths.get()
        taskData.javaHome = javaHome.get()
        taskData.targetPlatforms = targetPlatforms.get()
        taskData.jlinkJarsDir = jlinkJarsDir.get().asFile

        def taskImpl = new JlinkTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @OutputDirectory @PathSensitive(PathSensitivity.RELATIVE)
    File getImageDirAsFile() {
        imageName.get() ? imageDirFromName : imageDir.get().asFile
    }

    @Internal
    File getImageDirFromName() {
        project.file("$project.buildDir/${imageName.get()}")
    }

    @Internal
    String getDefaultMainClass() {
        def mainClass = project['mainClassName'] as String
        int pos = mainClass.lastIndexOf('/')
        if(pos < 0) return mainClass
        def mainClassModule = mainClass.substring(0, pos)
        if(mainClassModule != moduleName.get()) {
            LOGGER.warn("The module name specified in 'mainClassName' ($mainClassModule) has not the expected value (${moduleName.get()}).")
        }
        mainClass.substring(pos + 1)
    }
}

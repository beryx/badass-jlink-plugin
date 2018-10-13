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
import org.beryx.jlink.impl.JlinkTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class JlinkTask extends BaseTask {
    @Input
    Property<String> moduleName

    @Input
    Property<LauncherData> launcherData

    @Input
    Property<String> mainClass

    @Input
    Property<List<String>> options

    @Input
    Property<String> javaHome

    @InputDirectory
    DirectoryProperty jlinkJarsDir

    @OutputDirectory
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
        javaHome = extension.javaHome
        imageDir = extension.imageDir

        jlinkJarsDir = project.layout.directoryProperty()
        jlinkJarsDir.set(new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.imageDir = imageDir.get().asFile
        taskData.moduleName = moduleName.get()
        taskData.launcherData = launcherData.get()
        taskData.mainClass = mainClass.get() ?: project['mainClassName']
        taskData.options = options.get()
        taskData.javaHome = javaHome.get()
        taskData.jlinkJarsDir = jlinkJarsDir.get().asFile

        def taskImpl = new JlinkTaskImpl(project, taskData)
        taskImpl.execute()
    }
}

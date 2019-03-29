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
import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.data.JlinkTaskData
import org.beryx.jlink.impl.JPackageTaskImpl
import org.beryx.jlink.impl.JlinkTaskImpl
import org.beryx.jlink.util.PathUtil
import org.beryx.jlink.util.Util
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CompileStatic
class JPackageTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(JPackageTask.class);

    @Input
    Property<String> moduleName

    @Input
    Property<String> mainClass

    @InputDirectory
    DirectoryProperty jlinkJarsDir

    @Input
    Property<String> imageName

    @OutputDirectory
    DirectoryProperty imageDir

    @Nested
    Property<JPackageData> jpackageData

    JPackageTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates an installable image using the jpackage tool'
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        jpackageData = extension.jpackageData
        mainClass = extension.mainClass
        moduleName = extension.moduleName
        imageName = extension.imageName
        imageDir = extension.imageDir

        jlinkJarsDir = Util.createDirectoryProperty(project)
        jlinkJarsDir.set(new File(PathUtil.getJlinkJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void jpackageTaskAction() {
        def taskData = new JPackageTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.imageDir = imageName.get() ? imageDirFromName : imageDir.get().asFile
        taskData.moduleName = moduleName.get()
        taskData.jpackageData = jpackageData.get()
        taskData.mainClass = mainClass.get() ?: defaultMainClass
        taskData.jlinkJarsDir = jlinkJarsDir.get().asFile
        def JlinkTask jlinkTask = (JlinkTask) project.getTasksByName(JlinkPlugin.TASK_NAME_JLINK, false).first() // Should be one, and only one
        taskData.jlinkImageDir = jlinkTask.imageDirAsFile

        def taskImpl = new JPackageTaskImpl(project, taskData)
        taskImpl.execute()
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

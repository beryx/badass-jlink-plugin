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
import org.beryx.jlink.data.TargetPlatform
import org.beryx.jlink.util.PathUtil
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.tasks.Jar

@CompileStatic
abstract class AbstractJPackageTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(AbstractJPackageTask.class)

    @Internal
    abstract ListProperty<String> getDefaultJvmArgsProperty()

    @Internal
    abstract ListProperty<String> getDefaultArgsProperty()

    @Internal
    abstract Property<String> getProjectVersionProperty()

    @Internal
    abstract RegularFileProperty getProjectArchiveFileProperty()

    @Internal
    abstract MapProperty<String, TargetPlatform> getTargetPlatformsProperty()

    @Internal
    abstract Property<String> getEffectiveMainClassProperty()

    @groovy.transform.CompileDynamic
    AbstractJPackageTask() {
        project.getGradle().projectsEvaluated {
            defaultJvmArgsProperty.set(org.beryx.jlink.util.Util.getDefaultJvmArgs(project) ?: [])
            defaultArgsProperty.set(org.beryx.jlink.util.Util.getDefaultArgs(project) ?: [])
            projectVersionProperty.set(project.version.toString())
            projectArchiveFileProperty.set(project.tasks.named('jar', Jar).flatMap { it.archiveFile })
            targetPlatformsProperty.set(extension.targetPlatforms)

            def mc = extension.mainClass.getOrNull()
            if (!mc) {
                try {
                    mc = project.application?.mainClass?.getOrNull() as String
                    def mainModule = project.application?.mainModule?.getOrNull() as String
                    def moduleName = extension.moduleName.get()
                    if (mainModule && (mainModule != moduleName)) {
                        LOGGER.warn("The module name specified in 'application.mainModule' ($mainModule) has not the expected value ($moduleName).")
                    }
                } catch (Exception ignored) {
                    // application plugin not applied
                }
            }
            effectiveMainClassProperty.set(mc)
        }
    }

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
        projectLayout.projectDirectory.dir(PathUtil.getJlinkJarsDirPath(jlinkBasePath))
    }

    @Input
    String getImageName() {
        extension.imageName.get()
    }

    @InputDirectory
    File getImageInputDir() {
        extension.imageName.get() ? projectLayout.buildDirectory.file(extension.imageName.get()).get().asFile : extension.imageDir.get().asFile
    }

    @Nested
    JPackageData getJpackageData() {
        extension.jpackageData.get()
    }

    protected JPackageTaskData createTaskData() {
        def taskData = new JPackageTaskData()
        taskData.defaultJvmArgs = defaultJvmArgsProperty.get()
        taskData.defaultArgs = defaultArgsProperty.get()
        taskData.projectVersion = projectVersionProperty.get()
        taskData.projectArchiveFile = projectArchiveFileProperty.get().asFile
        taskData.jlinkBasePath = jlinkBasePath
        taskData.imageDir = imageInputDir
        taskData.moduleName = moduleName
        taskData.jpackageData = jpackageData
        taskData.mainClass = effectiveMainClassProperty.getOrNull()
        taskData.configureRuntimeImageDir(targetPlatformsProperty.getOrElse([:]), imageInputDir)
        taskData
    }
}

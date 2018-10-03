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
import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.data.ShowProspectiveMergedModuleInfoTaskData
import org.beryx.jlink.impl.ShowProspectiveMergedModuleInfoTaskImpl
import org.beryx.jlink.util.PathUtil
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@CompileStatic
class ShowProspectiveMergedModuleInfoTask extends BaseTask {
    @Input
    Property<List<String>> forceMergedJarPrefixes

    @InputDirectory
    DirectoryProperty mergedJarsDir

    @Input
    Property<String> javaHome

    @Input
    Property<JdepsUsage> useJdeps

    ShowProspectiveMergedModuleInfoTask() {
        dependsOn(JlinkPlugin.TASK_NAME_PREPARE_MERGED_JARS_DIR)
        description = 'Unpacks all non-modularized jars into a single directory'
        outputs.upToDateWhen { false }
    }

    @Override
    void init(JlinkPluginExtension extension) {
        super.init(extension)
        forceMergedJarPrefixes = extension.forceMergedJarPrefixes
        javaHome = extension.javaHome
        useJdeps = extension.useJdeps

        mergedJarsDir = project.layout.directoryProperty()
        mergedJarsDir.set(project.layout.buildDirectory.dir(PathUtil.getMergedJarsDirPath(jlinkBasePath.get())))
    }

    @TaskAction
    void showProspectiveMergedModuleInfoAction() {
        def taskData = new ShowProspectiveMergedModuleInfoTaskData()
        taskData.jlinkBasePath = jlinkBasePath.get()
        taskData.forceMergedJarPrefixes = forceMergedJarPrefixes.get()
        taskData.javaHome = javaHome.get()
        taskData.useJdeps = useJdeps.get()
        taskData.mergedJarsDir = mergedJarsDir.get().asFile
        taskData.jlinkJarsDirPath = PathUtil.getJlinkJarsDirPath(taskData.jlinkBasePath)
        taskData.tmpJarsDirPath = PathUtil.getTmpJarsDirPath(taskData.jlinkBasePath)

        def taskImpl = new ShowProspectiveMergedModuleInfoTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @Option(option = 'useJdeps', description = "Specifies whether jdeps should be used to generate the prospective module info. Accepted values: 'yes', 'no', 'exclusively'.")
    void setUseJdeps(String useJdeps) {
        try {
            this.useJdeps.set(JdepsUsage.valueOf(useJdeps))
        } catch (Exception e) {
            throw new GradleException("Unknown value for option 'useJdeps': $useJdeps. Accepted values: ${JdepsUsage.values()*.name().join(' / ')}.")
        }
    }
}

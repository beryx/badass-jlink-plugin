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

import org.beryx.jlink.impl.JlinkTaskData
import org.beryx.jlink.impl.JlinkTaskImpl
import org.beryx.jlink.impl.ModuleInfo
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class JlinkTask extends DefaultTask {
    @Input
    Property<String> moduleName

    @Input
    Property<String> launcherName

    @Input
    Property<String> mainClass

    @Input
    Property<String> mergedModuleName

    @Input
    Property<String> javaHome

    @Input
    Property<ModuleInfo> mergedModuleInfo

    @Input
    Property<String> jdepsEnabled

    @OutputDirectory
    DirectoryProperty imageDir

    JlinkTask() {
        dependsOn('jar')
        group = 'build'
        description = 'Creates modular runtime image with jlink'
    }

    @TaskAction
    void jlinkTaskAction() {
        def taskData = new JlinkTaskData()
        taskData.imageDir = imageDir.get().asFile
        taskData.moduleName = moduleName.get() ?: getDefaultModuleName()
        taskData.launcherName = launcherName.get() ?: project.name
        taskData.mainClass = mainClass.get() ?: project.mainClassName
        taskData.mergedModuleName = mergedModuleName.get() ?: getDefaultMergedModuleName()
        taskData.javaHome = javaHome.get() ?: System.getenv('JAVA_HOME')
        taskData.mergedModuleInfo = mergedModuleInfo.get()
        taskData.jdepsEnabled = jdepsEnabled.get()
        def taskImpl = new JlinkTaskImpl(project, taskData)
        taskImpl.execute()
    }

    private String getDefaultModuleName() {
        Set<File> srcDirs = project.sourceSets.main.java.srcDirs
        File moduleInfoDir = srcDirs.find { it.list().contains('module-info.java')}
        if(!moduleInfoDir) throw new GradleException("Cannot find module-info.java")
        String moduleInfoText = new File(moduleInfoDir, 'module-info.java').text
        getModuleNameFrom(moduleInfoText)
    }

    private String getDefaultMergedModuleName() {
        String name = (project.group ?: project.name) as String
        "${toModuleName(name)}.merged.module"
    }

    static String toModuleName(String s) {
        def name = s.replaceAll('[^0-9A-Za-z_.]', '.')
        int start = 0
        while(name[start] == '.') start++
        int end = name.length() - 1
        while(name[end] == '.') end--
        name = name.substring(start, end + 1)
        name.replaceAll('\\.[.]+', '.')
    }

    static String getModuleNameFrom(String moduleInfoText) {
        def matcher = moduleInfoText.readLines().collect {it =~ /\s*module\s+(\S+)\s*\{.*/}.find {it.matches()}
        if(matcher == null) throw new GradleException("Cannot retrieve module name from module-info.java")
        matcher[0][1]
    }
}

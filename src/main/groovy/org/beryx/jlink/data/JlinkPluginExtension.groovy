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
package org.beryx.jlink.data

import org.beryx.jlink.util.Util
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

class JlinkPluginExtension {
    final Property<String> jlinkBasePath
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip
    final Property<String> moduleName
    final Property<String> launcherName
    final Property<String> mainClass
    final Property<String> mergedModuleName
    final Property<List<String>> forceMergedJarPrefixes
    final Property<List<String>> options
    final Property<String> javaHome
    final Property<ModuleInfo> mergedModuleInfo
    final Property<Boolean> jdepsEnabled

    JlinkPluginExtension(Project project) {
        jlinkBasePath = project.objects.property(String)
        jlinkBasePath.set("$project.buildDir/jlinkbase" as String)

        imageDir = project.layout.directoryProperty()
        imageDir.set(new File(project.buildDir, 'image'))

        imageZip = project.layout.fileProperty()
        imageZip.set(new File(project.buildDir, 'image.zip'))

        moduleName = project.objects.property(String)
        moduleName.set(Util.getDefaultModuleName(project))

        launcherName = project.objects.property(String)
        launcherName.set(project.name)

        mainClass = project.objects.property(String)
        mainClass.set('')

        mergedModuleName = project.objects.property(String)
        mergedModuleName.set(Util.getDefaultMergedModuleName(project))

        forceMergedJarPrefixes = project.objects.property(List)
        forceMergedJarPrefixes.set(new ArrayList<String>())

        options = project.objects.property(List)
        options.set(new ArrayList<String>())

        javaHome = project.objects.property(String)
        javaHome.set(System.getenv('JAVA_HOME'))

        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())

        jdepsEnabled = project.objects.property(Boolean)
        jdepsEnabled.set(false)
    }

    void forceMerge(String... jarPrefixes) {
        forceMergedJarPrefixes.get().addAll(jarPrefixes)
    }

    void addOptions(String... jarPrefixes) {
        options.get().addAll(jarPrefixes)
    }

    void mergedModule(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = mergedModuleInfo.get()
        closure()
    }
}

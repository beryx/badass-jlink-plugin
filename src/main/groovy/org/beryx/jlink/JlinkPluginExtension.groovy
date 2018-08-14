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

import org.beryx.jlink.impl.ModuleInfo
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

class JlinkPluginExtension {
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip
    final Property<String> moduleName
    final Property<String> launcherName
    final Property<String> mainClass
    final Property<String> mergedModuleName
    final Property<String> javaHome
    final Property<ModuleInfo> mergedModuleInfo
    final Property<Boolean> jdepsEnabled
    final Property<Closure> beforeZipClosure

    JlinkPluginExtension(Project project) {
        imageDir = project.layout.directoryProperty()
        imageDir.set(new File(project.buildDir, 'image'))

        imageZip = project.layout.fileProperty()
        imageZip.set(new File(project.buildDir, 'image.zip'))

        beforeZipClosure = project.objects.property(Closure)

        moduleName = project.objects.property(String)
        moduleName.set('')

        launcherName = project.objects.property(String)
        launcherName.set('')

        mainClass = project.objects.property(String)
        mainClass.set('')

        mergedModuleName = project.objects.property(String)
        mergedModuleName.set('')

        javaHome = project.objects.property(String)
        javaHome.set('')

        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())

        jdepsEnabled = project.objects.property(Boolean)
        jdepsEnabled.set(false)
    }

    void mergedModule(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = mergedModuleInfo.get()
        closure()
    }

    void beforeZip(Closure closure) {
        this.beforeZipClosure.set(closure)
    }
}

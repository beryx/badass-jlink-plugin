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
import org.gradle.api.provider.Property

class JlinkPluginExtension {
    final DirectoryProperty imageDir
    final Property<String> moduleName
    final Property<String> launcherName
    final Property<String> mainClass
    final Property<String> mergedModuleName
    final Property<String> javaHome
    final Property<ModuleInfo> mergedModuleInfo

    JlinkPluginExtension(Project project) {
        imageDir = project.layout.directoryProperty()
        moduleName = project.objects.property(String)
        launcherName = project.objects.property(String)
        mainClass = project.objects.property(String)
        mergedModuleName = project.objects.property(String)
        javaHome = project.objects.property(String)
        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())
    }

    void mergedModule(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = mergedModuleInfo.get()
        closure()
    }
}

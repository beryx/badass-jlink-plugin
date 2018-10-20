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

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.beryx.jlink.util.Util
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@CompileStatic
@ToString(includeNames = true)
class JlinkPluginExtension {
    final Property<String> jlinkBasePath
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip
    final Property<String> moduleName
    final Property<String> mergedModuleName

    final Property<LauncherData> launcherData
    final Property<String> mainClass
    final ListProperty<String> forceMergedJarPrefixes
    final ListProperty<String> extraDependenciesPrefixes
    final ListProperty<String> options
    final Property<ModuleInfo> mergedModuleInfo
    final Property<JdepsUsage> useJdeps
    final Property<String> javaHome
    final Property<Map<String, TargetPlatform>> targetPlatforms
    final Property<Integer> jvmVersion


    JlinkPluginExtension(Project project) {
        project.provider{}
        jlinkBasePath = project.objects.property(String)
        jlinkBasePath.set(project.provider{"$project.buildDir/jlinkbase" as String})

        imageDir = project.objects.directoryProperty()
        imageDir.set(project.layout.buildDirectory.dir('image'))

        imageZip = project.objects.fileProperty()
        imageZip.set(project.layout.buildDirectory.file('image.zip'))

        moduleName = project.objects.property(String)
        moduleName.set(project.provider{Util.getDefaultModuleName(project)})

        mergedModuleName = project.objects.property(String)
        mergedModuleName.set(project.provider{Util.getDefaultMergedModuleName(project)})

        launcherData = project.objects.property(LauncherData)
        def ld = new LauncherData()
        ld.name = project.name
        launcherData.set(ld)

        mainClass = project.objects.property(String)
        mainClass.set('')

        forceMergedJarPrefixes = project.objects.listProperty(String)
        forceMergedJarPrefixes.set(new ArrayList<String>())

        extraDependenciesPrefixes = project.objects.listProperty(String)
        extraDependenciesPrefixes.set(new ArrayList<String>())

        options = project.objects.listProperty(String)
        options.set(new ArrayList<String>())

        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())

        useJdeps = project.objects.property(JdepsUsage)
        useJdeps.set(JdepsUsage.no)

        javaHome = project.objects.property(String)
        javaHome.set(System.getenv('JAVA_HOME'))

        targetPlatforms = (Property)project.objects.property(Map)
        targetPlatforms.set(new TreeMap<>())

        jvmVersion = project.objects.property(Integer)
    }

    void addExtraDependencies(String... jarPrefixes) {
        extraDependenciesPrefixes.get().addAll(jarPrefixes)
    }

    void forceMerge(String... jarPrefixes) {
        forceMergedJarPrefixes.get().addAll(jarPrefixes)
    }

    void addOptions(String... options) {
        this.options.get().addAll(options)
    }

    void targetPlatform(String name, String jdkHome, List<String> options = []) {
        targetPlatforms.get()[name] = new TargetPlatform(name, jdkHome, options)
    }

    void mergedModule(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = mergedModuleInfo.get()
        mergedModuleInfo.get().enabled = true
        closure()
    }

    void launcher(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = launcherData.get()
        closure()
    }
}

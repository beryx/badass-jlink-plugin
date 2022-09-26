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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class JlinkPluginExtension {
    private final Project project
    final Property<String> jlinkBasePath
    final Property<String> imageName
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip
    final Property<String> moduleName
    final Property<String> mergedModuleName
    final Property<String> mergedModuleJarName
    final Property<CdsData> cdsData

    /** @deprecated - use  {@link ModuleInfo#version} instead */
    @Deprecated(since = "2.22.1", forRemoval = true)
    final Property<String> mergedModuleJarVersion

    final Property<LauncherData> launcherData
    final ListProperty<SecondaryLauncherData> secondaryLaunchers
    final Property<String> mainClass
    final Property<String> configuration
    final ListProperty<String> forceMergedJarPrefixes
    final ListProperty<String> extraDependenciesPrefixes
    final ListProperty<String> extraModulePaths
    final ListProperty<String> options
    final Property<ModuleInfo> mergedModuleInfo
    final Property<JdepsUsage> useJdeps
    final Property<String> javaHome
    final Provider<Map<String, TargetPlatform>> targetPlatforms
    final Property<Integer> jvmVersion
    final Property<CustomImageData> customImageData

    final Property<JPackageData> jpackageData
    final Provider<Map<String, List<String>>> jarExcludes

    JlinkPluginExtension(Project project) {
        this.project = project
        jlinkBasePath = project.objects.property(String)
        jlinkBasePath.set(project.provider{"$project.buildDir/jlinkbase" as String})

        imageName = project.objects.property(String)
        imageName.set('')

        imageDir = Util.createDirectoryProperty(project)
        imageDir.set(project.layout.buildDirectory.dir('image'))

        imageZip = Util.createRegularFileProperty(project)
        imageZip.set(project.layout.buildDirectory.file('image.zip'))

        moduleName = project.objects.property(String)
        moduleName.set(project.provider{Util.getDefaultModuleName(project)})

        mergedModuleName = project.objects.property(String)
        mergedModuleName.set(project.provider{Util.getDefaultMergedModuleName(project)})

        mergedModuleJarName = project.objects.property(String)
        mergedModuleJarName.set(project.provider{"${Util.getArchiveBaseName(project)}.merged.module.jar" as String})

        mergedModuleJarVersion = project.objects.property(String)
        mergedModuleJarVersion.set(project.provider{project.version as String})

        launcherData = project.objects.property(LauncherData)
        def ld = new LauncherData(project.name)
        launcherData.set(ld)

        customImageData = project.objects.property(CustomImageData)
        customImageData.set(new CustomImageData())

        secondaryLaunchers = project.objects.listProperty(SecondaryLauncherData)
        secondaryLaunchers.set(new ArrayList<SecondaryLauncherData>())

        mainClass = project.objects.property(String)
        mainClass.set('')

        configuration = project.objects.property(String)
        configuration.set('runtimeClasspath')

        forceMergedJarPrefixes = project.objects.listProperty(String)
        forceMergedJarPrefixes.set(new ArrayList<String>())

        extraDependenciesPrefixes = project.objects.listProperty(String)
        extraDependenciesPrefixes.set(new ArrayList<String>())

        extraModulePaths = project.objects.listProperty(String)
        extraModulePaths.set(new ArrayList<String>())

        options = project.objects.listProperty(String)
        options.set(new ArrayList<String>())

        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())

        useJdeps = project.objects.property(JdepsUsage)
        useJdeps.set(JdepsUsage.no)

        javaHome = project.objects.property(String)

        targetPlatforms = Util.createMapProperty(project, String, TargetPlatform)

        jvmVersion = project.objects.property(Integer)

        jpackageData = project.objects.property(JPackageData)
        def jpd = new JPackageData(project, ld)
        jpackageData.set(jpd)

        cdsData = project.objects.property(CdsData)
        cdsData.set(new CdsData())

        jarExcludes = Util.createMapProperty(project, String, List) as Provider<Map<String, List<String>>>
    }

    void jarExclude(String jarPrefix, String... excludePatterns) {
        Util.putToMapProvider(jarExcludes, jarPrefix, excludePatterns as List)
    }

    void addExtraDependencies(String... dependencies) {
        Util.addToListProperty(extraDependenciesPrefixes, dependencies)
    }

    void addExtraModulePath(String path) {
        Util.addToListProperty(extraModulePaths, path)
    }

    void forceMerge(String... jarPrefixes) {
        Util.addToListProperty(forceMergedJarPrefixes, jarPrefixes)
    }

    void addOptions(String... options) {
        Util.addToListProperty(this.options, options)
    }

    void targetPlatform(String name, String jdkHome, List<String> options = []) {
        Util.putToMapProvider(targetPlatforms, name, new TargetPlatform(project, name, jdkHome, options))
    }

    void targetPlatform(String name, Action<TargetPlatform> action) {
        def targetPlatform = new TargetPlatform(project, name)
        action.execute(targetPlatform)
        Util.putToMapProvider(targetPlatforms, name, targetPlatform)
    }

    void mergedModule(Action<ModuleInfo> action) {
        mergedModuleInfo.get().enabled = true
        action.execute(mergedModuleInfo.get())
    }

    void launcher(Action<LauncherData> action) {
        action.execute(launcherData.get())
    }

    void secondaryLauncher(Action<LauncherData> action) {
        def ld = new SecondaryLauncherData(null)
        ld.moduleName = moduleName.get()
        Util.addToListProperty(secondaryLaunchers, ld)
        action.execute(ld)
        ld.check()
        jpackageData.get().addSecondaryLauncher(ld)
    }

    void customImage(Action<CustomImageData> action = null) {
        customImageData.get().enabled = true
        if(action) {
            action.execute(customImageData.get())
        }
    }

    void jpackage(Action<JPackageData> action) {
        action.execute(jpackageData.get())
    }

    void enableCds(Action<CdsData> action = null) {
        cdsData.get().enabled = true
        if(action) {
            action.execute(cdsData.get())
        }
    }
}

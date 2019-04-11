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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
@ToString(includeNames = true)
class JlinkPluginExtension {
    final Property<String> jlinkBasePath
    final Property<String> imageName
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip
    final Property<String> moduleName
    final Property<String> mergedModuleName

    final Property<LauncherData> launcherData
    final Property<String> mainClass
    final ListProperty<String> forceMergedJarPrefixes
    final ListProperty<String> extraDependenciesPrefixes
    final ListProperty<String> extraModulePaths
    final ListProperty<String> options
    final Property<ModuleInfo> mergedModuleInfo
    final Property<JdepsUsage> useJdeps
    final Property<String> javaHome
    final Provider<Map<String, TargetPlatform>> targetPlatforms
    final Property<Integer> jvmVersion

    final Property<JPackageData> jpackageData

    JlinkPluginExtension(Project project) {
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

        extraModulePaths = project.objects.listProperty(String)
        extraModulePaths.set(new ArrayList<String>())

        options = project.objects.listProperty(String)
        options.set(new ArrayList<String>())

        mergedModuleInfo = project.objects.property(ModuleInfo)
        mergedModuleInfo.set(new ModuleInfo())

        useJdeps = project.objects.property(JdepsUsage)
        useJdeps.set(JdepsUsage.no)

        javaHome = project.objects.property(String)
        javaHome.set(getDefaultJavaHome())

        targetPlatforms = Util.createMapProperty(project, String, TargetPlatform)

        jvmVersion = project.objects.property(Integer)

        jpackageData = project.objects.property(JPackageData)
        def jpd = new JPackageData(project, ld)
        jpackageData.set(jpd)
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
        Util.putToMapProvider(targetPlatforms, name, new TargetPlatform(name, jdkHome, options))
    }

    void targetPlatform(String name, Action<TargetPlatform> action) {
        def targetPlatform = new TargetPlatform(name)
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

    void jpackage(Action<JPackageData> action) {
        action.execute(jpackageData.get())
    }

    private static String getDefaultJavaHome() {
        def value = System.properties['badass.jlink.java.home']
        if(value) return value
        value = System.getenv('BADASS_JLINK_JAVA_HOME')
        if(value) return value
        value = System.properties['java.home']
        if(['javac', 'jar', 'jlink'].every { new File("$value/bin/$it$EXEC_EXTENSION").file }) return value
        return System.getenv('JAVA_HOME')
    }
}

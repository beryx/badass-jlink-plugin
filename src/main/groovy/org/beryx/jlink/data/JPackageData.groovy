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
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

@CompileStatic
@ToString(includeNames = true)
class JPackageData {
    private final Project project
    private final LauncherData launcherData
    private final Provider<Directory> javaHomeProvider
    final ListProperty<SecondaryLauncherData> secondaryLaunchers

    final Property<String> jpackageHome
    final Property<String> outputDir
    final DirectoryProperty imageOutputDir
    final Property<String> imageName
    final ListProperty<String> imageOptions
    final DirectoryProperty resourceDir
    final Property<String> targetPlatformName
    final Property<Boolean> skipInstaller
    final Property<String> installerType
    final DirectoryProperty installerOutputDir
    final Property<String> installerName
    final Property<String> appVersion
    final Property<String> vendor
    final Property<String> icon
    final ListProperty<String> installerOptions
    final ListProperty<String> args
    final ListProperty<String> jvmArgs

    JPackageData(Project project, LauncherData launcherData, Provider<Directory> javaHomeProvider) {
        this.project = project
        this.launcherData = launcherData
        this.javaHomeProvider = javaHomeProvider

        jpackageHome = project.objects.property(String)
        jpackageHome.convention('')

        outputDir = project.objects.property(String)
        outputDir.convention('jpackage')

        imageOutputDir = project.objects.directoryProperty()
        imageOutputDir.convention(project.layout.buildDirectory.dir(outputDir.map { it }))

        imageName = project.objects.property(String)
        imageName.convention(project.provider { launcherData.name ?: project.name })

        imageOptions = project.objects.listProperty(String)
        imageOptions.convention([])

        resourceDir = project.objects.directoryProperty()

        targetPlatformName = project.objects.property(String)

        skipInstaller = project.objects.property(Boolean)
        skipInstaller.convention(false)

        installerType = project.objects.property(String)

        installerOutputDir = project.objects.directoryProperty()
        installerOutputDir.convention(project.layout.buildDirectory.dir(outputDir.map { it }))

        installerName = project.objects.property(String)
        installerName.convention(project.provider { launcherData.name ?: project.name })

        appVersion = project.objects.property(String)

        vendor = project.objects.property(String)
        vendor.convention('Unknown')

        icon = project.objects.property(String)

        installerOptions = project.objects.listProperty(String)
        installerOptions.convention([])

        args = project.objects.listProperty(String)
        args.convention(project.provider { launcherData.getEffectiveArgs(project) })

        jvmArgs = project.objects.listProperty(String)
        jvmArgs.convention(project.provider { launcherData.getEffectiveJvmArgs(project) })

        secondaryLaunchers = project.objects.listProperty(SecondaryLauncherData)
        secondaryLaunchers.convention([])
    }

    @Input
    String getJpackageHome() {
        jpackageHome.get()
    }

    void setJpackageHome(String jpackageHome) {
        this.jpackageHome.set(jpackageHome)
    }

    @Input
    String getOutputDir() {
        outputDir.get()
    }

    void setOutputDir(String outputDir) {
        this.outputDir.set(outputDir)
    }

    @OutputDirectory
    File getImageOutputDir() {
        imageOutputDir.get().asFile
    }

    void setImageOutputDir(Object imageOutputDir) {
        this.imageOutputDir.set(project.file(imageOutputDir))
    }

    @Input
    String getImageName() {
        imageName.get()
    }

    void setImageName(String imageName) {
        this.imageName.set(imageName)
    }

    @Input
    List<String> getImageOptions() {
        imageOptions.get()
    }

    void setImageOptions(List<String> imageOptions) {
        this.imageOptions.set(imageOptions)
    }

    @InputDirectory @Optional
    File getResourceDir() {
        resourceDir.asFile.getOrNull()
    }

    void setResourceDir(Object resourceDir) {
        this.resourceDir.set(project.file(resourceDir))
    }

    @Input @Optional
    String getTargetPlatformName() {
        targetPlatformName.getOrNull()
    }

    void setTargetPlatformName(String targetPlatformName) {
        this.targetPlatformName.set(targetPlatformName)
    }

    @Input
    boolean getSkipInstaller() {
        skipInstaller.get()
    }

    @Internal
    boolean isSkipInstaller() {
        skipInstaller.get()
    }

    void setSkipInstaller(boolean skipInstaller) {
        this.skipInstaller.set(skipInstaller)
    }

    @Input @Optional
    String getInstallerType() {
        installerType.getOrNull()
    }

    void setInstallerType(String installerType) {
        this.installerType.set(installerType)
    }

    @OutputDirectory
    File getInstallerOutputDir() {
        installerOutputDir.get().asFile
    }

    void setInstallerOutputDir(Object installerOutputDir) {
        this.installerOutputDir.set(project.file(installerOutputDir))
    }

    @Input
    String getInstallerName() {
        installerName.get()
    }

    void setInstallerName(String installerName) {
        this.installerName.set(installerName)
    }

    @Input @Optional
    String getAppVersion() {
        appVersion.getOrNull()
    }

    void setAppVersion(String appVersion) {
        this.appVersion.set(appVersion)
    }

    @Input
    String getVendor() {
        vendor.get()
    }

    void setVendor(String vendor) {
        this.vendor.set(vendor)
    }

    @Input @Optional
    String getIcon() {
        icon.getOrNull()
    }

    void setIcon(String icon) {
        this.icon.set(icon)
    }

    @Input
    List<String> getInstallerOptions() {
        installerOptions.get()
    }

    void setInstallerOptions(List<String> installerOptions) {
        this.installerOptions.set(installerOptions)
    }

    @Input
    List<String> getArgs() {
        args.get()
    }

    void setArgs(List<String> args) {
        this.args.set(args)
    }

    @Input
    List<String> getJvmArgs() {
        jvmArgs.get()
    }

    void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs.set(jvmArgs)
    }

    @Input
    String getLauncherName() {
        launcherData.name
    }

    @Input
    List<SecondaryLauncherData> getSecondaryLaunchers() {
        secondaryLaunchers.get()
    }

    void addSecondaryLauncher(SecondaryLauncherData launcher) {
        secondaryLaunchers.add(launcher)
    }

    @Internal
    String getJPackageHomeOrDefault() {
        return getJpackageHome() ?: defaultJPackageHome
    }

    @Internal
    String getDefaultJPackageHome() {
        def value = System.properties['badass.jlink.jpackage.home']
        if(value) return value
        value = System.getenv('BADASS_JLINK_JPACKAGE_HOME')
        if(value) return value

        def jpackageHomeFolder = javaHomeProvider.getOrNull()?.asFile ?:
                new File(System.getenv('JAVA_HOME'))

        jpackageHomeFolder.absolutePath
    }
}

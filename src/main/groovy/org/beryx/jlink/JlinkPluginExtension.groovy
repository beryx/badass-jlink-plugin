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

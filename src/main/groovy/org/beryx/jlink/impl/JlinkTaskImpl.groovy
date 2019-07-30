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
package org.beryx.jlink.impl

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jlink.data.JlinkTaskData
import org.beryx.jlink.util.LaunchScriptGenerator
import org.beryx.jlink.util.SuggestedModulesBuilder
import org.beryx.jlink.util.Util
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.module.ModuleDescriptor

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JlinkTaskImpl extends BaseTaskImpl<JlinkTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JlinkZipTaskImpl.class);

    JlinkTaskImpl(Project project, JlinkTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        if(td.targetPlatforms) {
            td.targetPlatforms.values().each { platform ->
                File imageDir = new File(td.imageDir, "$td.launcherData.name-$platform.name")
                runJlink(imageDir,
                        platform.jdkHome ?: td.javaHome,
                        td.extraModulePaths + platform.extraModulePaths,
                        td.options + platform.options)
                createLaunchScripts(imageDir)
            }
        } else {
            runJlink(td.imageDir, td.javaHome, td.extraModulePaths, td.options)
            createLaunchScripts(td.imageDir)
        }
    }

    @CompileDynamic
    void runJlink(File imageDir, String jdkHome, List<String> extraModulePaths, List<String> options) {
        project.delete(imageDir)
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jlinkOutput = {
                return standardOutput.toString()
            }
            def jlinkJarsDirAsPath = project.files(td.jlinkJarsDir).asPath
            def additionalModulePaths = extraModulePaths.collect {SEP + it}.join('')
            def jlinkExec = "$td.javaHome/bin/jlink$EXEC_EXTENSION"
            Util.checkExecutable(jlinkExec)
            commandLine = [jlinkExec,
                           '-v',
                           *options,
                           '--module-path', "$jdkHome/jmods/$additionalModulePaths$SEP$jlinkJarsDirAsPath",
                           '--add-modules', imageModules.join(','),
                           '--output', imageDir]
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jlinkOutput())
        } else {
            LOGGER.info(project.ext.jlinkOutput())
            copyNonImageModules(imageDir, extraModulePaths + [td.jlinkJarsDir.path])
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    static class ModuleData {
        String name
        File file
        Set<String> requires

        ModuleData(File file, ModuleDescriptor md) {
            this.file = file
            this.name = md.name()
            this.requires = new HashSet(md.requires().collect {it.name()})
        }
    }

    @CompileDynamic
    private void copyNonImageModules(File imageDir, List<String> modulePaths) {
        if (td.customImageData.enabled) {
            Map<String, ModuleData> moduleData = [:]
            Util.getJarsAndMods(modulePaths.toArray()).each { file ->
                ModuleDescriptor md = Util.getModuleDescriptor(file)
                if(md) moduleData[md.name()] = new ModuleData(file, md)
            }
            Set<String> transitiveModules = moduleData.keySet()
            LOGGER.info "transitiveModules: $transitiveModules"

            Set<String> transitiveImageModules = []
            imageModules.each { addTransitive(transitiveImageModules, it, moduleData) }
            LOGGER.info "transitiveImageModules: $transitiveImageModules"

            Set<String> transitiveNonImageModules = (transitiveModules as Set<String>) - transitiveImageModules
            LOGGER.info "transitiveNonImageModules: $transitiveNonImageModules"
            if(transitiveNonImageModules) {
                new File(imageDir, 'app').mkdirs()
                project.copy {
                    into "$imageDir/app"
                    from transitiveNonImageModules.collect { moduleData[it].file }.toArray()
                }
            }
        }
    }

    private static void excludeTransitive(Set<String> nonImageModules, String moduleName, Map<String, ModuleData> moduleData) {
        def childMd = moduleData[moduleName]
        if(childMd) {
            nonImageModules.remove(moduleName)
            if(nonImageModules.empty) return
            childMd.requires.each { excludeTransitive(nonImageModules, it, moduleData) }
        }
    }

    private static void addTransitive(Set<String> requiredModules, String moduleName, Map<String, ModuleData> moduleData) {
        if(!requiredModules.contains(moduleName)) {
            def childMd = moduleData[moduleName]
            if(childMd) {
                requiredModules << moduleName
                childMd.requires.each { addTransitive(requiredModules, it, moduleData) }
            }
        }
    }

    Collection<String> getJdkModules() {
        if(td.customImageData.jdkAdditive) {
            if(td.customImageData.jdkModules ) {
                new SuggestedModulesBuilder(td.javaHome).getProjectModules(project) + td.customImageData.jdkModules
            } else {
                new SuggestedModulesBuilder(td.javaHome).getProjectModules(project)
            }
        } else {
            td.customImageData.jdkModules ?: new SuggestedModulesBuilder(td.javaHome).getProjectModules(project)
        }
    }

    Collection<String> getImageModules() {
        if(td.customImageData.enabled) {
            return appModules + jdkModules
        } else {
            return [td.moduleName]
        }
    }

    Collection<String> getAppModules() {
        td.customImageData.appModules
    }

    void createLaunchScripts(File imageDir) {
        def generator = new LaunchScriptGenerator(td.moduleName, td.mainClass, td.launcherData)
        generator.generate("$imageDir/bin")
        td.secondaryLaunchers.each { launcher ->
            def secondaryGenerator = new LaunchScriptGenerator(launcher.moduleName, launcher.mainClass, launcher)
            secondaryGenerator.generate("$imageDir/bin")
        }
    }
}

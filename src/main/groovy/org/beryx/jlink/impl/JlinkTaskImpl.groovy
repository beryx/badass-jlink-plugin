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
import org.beryx.jlink.util.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JlinkTaskImpl extends BaseTaskImpl<JlinkTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JlinkZipTaskImpl.class);

    static class ModuleData {
        String name
        File file
        Set<String> requires
    }

    JlinkTaskImpl(Project project, JlinkTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        if(td.cdsData.enabled && td.customImageData.enabled) {
            System.properties['BADASS_CDS_ARCHIVE_FILE_LINUX'] = td.cdsData.sharedArchiveFile ?: '$DIR/../lib/server/$APP_NAME.jsa'
            System.properties['BADASS_CDS_ARCHIVE_FILE_WINDOWS'] = td.cdsData.sharedArchiveFile ?: '%~dp0\\server\\%~n0.jsa'
        }
        if(td.targetPlatforms) {
            td.targetPlatforms.values().each { platform ->
                File imageDir = new File(td.imageDir, "$td.launcherData.name-$platform.name")
                createRuntimeImage(imageDir,
                        platform.jdkHome ?: td.javaHome,
                        td.extraModulePaths + platform.extraModulePaths,
                        td.options + platform.options)
            }
        } else {
            createRuntimeImage(td.imageDir, td.javaHome, td.extraModulePaths, td.options)
        }
    }

    void createRuntimeImage(File imageDir, String jdkHome, List<String> extraModulePaths, List<String> options) {
        runJlink(imageDir, jdkHome, extraModulePaths, options)
        createCDSArchive(imageDir)
        createLaunchScripts(imageDir)
    }

    @CompileDynamic
    void createCDSArchive(File imageDir) {
        if(td.cdsData.enabled) {
            project.services.get(org.gradle.process.ExecOperations).exec { spec ->
                spec.commandLine = ["$imageDir/bin/java", "-Xshare:dump"]
            }
        }
    }

    @CompileDynamic
    void runJlink(File imageDir, String jdkHome, List<String> extraModulePaths, List<String> options) {
        if(!new File("$jdkHome/jmods/java.base.jmod").file && JavaVersion.get(jdkHome) < 24) {
            throw new GradleException("java.base module not found in $jdkHome${File.separator}jmods")
        }
        project.delete(imageDir)
        def result = {
            def execOps = project.services.get(org.gradle.process.ExecOperations)

            def outputStream = new ByteArrayOutputStream()

            def jlinkJarsDirAsPath = project.files(td.jlinkJarsDir).asPath
            def additionalModulePaths = extraModulePaths.collect {SEP + it}.join('')
            def jlinkExec = "$td.javaHome/bin/jlink$EXEC_EXTENSION"
            Util.checkExecutable(jlinkExec)

            def execResult = execOps.exec { spec ->
                spec.ignoreExitValue = true
                spec.standardOutput = outputStream
                spec.commandLine = [
                        jlinkExec,
                        '-v',
                        *options,
                        '--module-path', "$jdkHome/jmods/$additionalModulePaths$SEP$jlinkJarsDirAsPath",
                        '--add-modules', imageModules.join(','),
                        '--output', imageDir
                ]
            }

            project.ext.jlinkOutput = {
                return outputStream.toString()
            }

            // Return the exec result
            return execResult
        }()
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jlinkOutput())
        } else {
            LOGGER.info(project.ext.jlinkOutput())
            copyNonImageModules(imageDir, extraModulePaths + [td.jlinkJarsDir.path])
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    @CompileDynamic
    private void copyNonImageModules(File imageDir, List<String> modulePaths) {
        if (td.customImageData.enabled) {
            Map<String, ModuleData> moduleData = [:]
            Util.getJarsAndMods(modulePaths.toArray()).each { file ->
                ModuleData md = getModuleData(file)
                if(md) moduleData[md.name] = md
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
                new SuggestedModulesBuilder(td.javaHome, td.configuration).projectModules + td.customImageData.jdkModules
            } else {
                new SuggestedModulesBuilder(td.javaHome, td.configuration).projectModules
            }
        } else {
            td.customImageData.jdkModules ?: new SuggestedModulesBuilder(td.javaHome, td.configuration).projectModules
        }
    }

    Collection<String> getImageModules() {
        if(td.customImageData.enabled) {
            return appModules + jdkModules
        } else {
            return [td.moduleName, td.mergedModuleName]
        }
    }

    Collection<String> getAppModules() {
        td.customImageData.appModules
    }

    void createLaunchScripts(File imageDir) {
        def generator = new LaunchScriptGenerator(project, td.moduleName, td.mainClass, td.launcherData)
        generator.generate("$imageDir/bin")
        td.secondaryLaunchers.each { launcher ->
            def moduleName = launcher.moduleName ? launcher.moduleName : td.moduleName
            def mainClass = launcher.mainClass ? launcher.mainClass : td.mainClass
            def secondaryGenerator = new LaunchScriptGenerator(project, moduleName, mainClass, launcher)
            secondaryGenerator.generate("$imageDir/bin")
        }
    }

    private ModuleData getModuleData(File f) {
        def runner = new SourceCodeRunner(td.javaHome, 'ModuleData', sourceCode)
        def output = runner.getOutput(f.absolutePath)
        if(!output) return null
        def tokens = output.split(':')
        if(tokens.length != 2) {
            LOGGER.warn("getModuleData($f): output contains $tokens.length  + tokens: " + output)
            return null
        }
        ModuleData moduleData = new ModuleData()
        moduleData.file = f
        moduleData.name = tokens[0]
        moduleData.requires = new HashSet<>(tokens[1].split(',').collect())
        return moduleData
    }

    static final String sourceCode = """
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModuleData {
    public static void main(String[] args) {
        if(args.length != 1) throw new IllegalArgumentException("ModuleData expects one argument.");
        File f = new File(args[0]);
        ModuleDescriptor md = getModuleDescriptor(f);
        if(md != null && !md.exports().isEmpty()) {
            String requires = md.requires().stream()
                    .map(ModuleDescriptor.Requires::name)
                    .collect(Collectors.joining(","));
            System.out.println(md.name() + ":" + requires);
        }
    }
    $SourceCodeConstants.GET_MODULE_DESCRIPTOR
}
"""
}

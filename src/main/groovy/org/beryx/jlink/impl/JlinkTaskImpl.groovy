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
            if(td.customImageData.enabled) {
                commandLine = [jlinkExec,
                               '-v',
                               *options,
                               '--module-path', "$jdkHome/jmods/",
                               '--add-modules', jdkModules.join(','),
                               '--output', imageDir]
            } else {
                commandLine = [jlinkExec,
                               '-v',
                               *options,
                               '--module-path', "$jdkHome/jmods/$additionalModulePaths$SEP$jlinkJarsDirAsPath",
                               '--add-modules', td.moduleName,
                               '--output', imageDir]
            }
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jlinkOutput())
        } else {
            LOGGER.info(project.ext.jlinkOutput())

            new File(imageDir, 'app').mkdirs()
            if(td.customImageData.enabled) {
                project.copy {
                    from td.jlinkJarsDir
                    into "$imageDir/app"
                }
            }
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    Collection<String> getJdkModules() {
        td.customImageData.jdkModules ?: new SuggestedModulesBuilder(td.javaHome).getProjectModules(project)
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

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
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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
                runJlink(imageDir, platform.jdkHome, td.options + platform.options)
                createLaunchScripts(imageDir)
            }
        } else {
            runJlink(td.imageDir, td.javaHome, td.options)
            createLaunchScripts(td.imageDir)
        }
    }

    @CompileDynamic
    void runJlink(File imageDir, String jdkHome, List<String> options) {
        project.delete(imageDir)
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jlinkOutput = {
                return standardOutput.toString()
            }
            commandLine = ["$td.javaHome/bin/jlink",
                           '-v',
                           *options,
                           '--module-path',
                           "$jdkHome/jmods/$SEP${project.files(td.jlinkJarsDir).asPath}",
                           '--add-modules', td.moduleName,
                           '--output', imageDir]
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jlinkOutput())
        } else {
            LOGGER.info(project.ext.jlinkOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    void createLaunchScripts(File imageDir) {
        def generator = new LaunchScriptGenerator(td.moduleName, td.mainClass, td.launcherData)
        generator.generate("$imageDir/bin")
    }
}

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
import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.data.SuggestMergedModuleInfoTaskData
import org.beryx.jlink.util.JdepsExecutor
import org.beryx.jlink.util.SuggestedMergedModuleInfoBuilder
import org.beryx.jlink.util.SuggestedModulesBuilder
import org.beryx.jlink.util.Util
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class SuggestMergedModuleInfoTaskImpl extends BaseTaskImpl<SuggestMergedModuleInfoTaskData> {
    private static final Logger LOGGER = Logging.getLogger(SuggestMergedModuleInfoTaskImpl.class);

    SuggestMergedModuleInfoTaskImpl(Project project, SuggestMergedModuleInfoTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        LOGGER.info("Executing suggestMergedModuleInfo with useJdeps = $td.useJdeps")
        boolean skipBuilder = false
        if(td.useJdeps != JdepsUsage.no) {
            skipBuilder = printJdepsModuleInfo() || (td.useJdeps == JdepsUsage.exclusively)
        }
        if(!skipBuilder) {
            def builder = new SuggestedMergedModuleInfoBuilder(
                    project: project,
                    mergedJarsDir: td.mergedJarsDir,
                    javaHome: td.javaHome,
                    forceMergedJarPrefixes: td.forceMergedJarPrefixes,
                    extraDependenciesPrefixes: td.extraDependenciesPrefixes,
                    configuration: td.configuration,
                    constraints: td.additiveConstraints
            )
            println "mergedModule {\n${builder.moduleInfo.toString(4, td.language)}\n}"
        }
        if(td.customImageEnabled) {
            def modules = new SuggestedModulesBuilder(td.javaHome, td.configuration).projectModules
            println """
                customImage {
                    jdkModules = [${modules.join(', ')}]
                }
            """.stripIndent()
        }
    }

    @CompileDynamic
    private boolean printJdepsModuleInfo() {
        try {
            def jarFilePath = "$td.jlinkBasePath/suggestedMergedModule.jar"
            new File(jarFilePath).delete()
            Util.createJar(project, td.javaHome, jarFilePath, td.mergedJarsDir)
            def result = new JdepsExecutor(project).genModuleInfo(project.file(jarFilePath),
                    project.file(td.tmpJarsDirPath), td.jlinkJarsDirPath, td.javaHome)
            def loggerFun = result.exitValue ? (td.useJdeps == JdepsUsage.yes) ? 'warn' : 'error' : 'info'
            LOGGER."$loggerFun"(result.output)
            if (result.exitValue) {
                if (td.useJdeps == JdepsUsage.exclusively) {
                    throw new GradleException("jdeps exited with return code $result.exitValue")
                }
                return false
            } else {
                println "jdeps generated module-info.java:\n${result.moduleInfoFile?.text}"
                return true
            }
        } catch (Exception e) {
            if (td.useJdeps == JdepsUsage.exclusively) {
                throw new GradleException("jdeps failed", e)
            }
            if (LOGGER.infoEnabled) {
                LOGGER.info("jdeps failed.", e)
            } else {
                LOGGER.warn("jdeps failed: $e")
            }
            return false
        }
    }
}

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

import org.beryx.jlink.data.JdepsUsage
import org.beryx.jlink.data.ModuleInfo
import org.beryx.jlink.data.SuggestMergedModuleInfoTaskData
import org.beryx.jlink.util.JdepsExecutor
import org.beryx.jlink.util.SuggestedMergedModuleInfoBuilder
import org.beryx.jlink.util.Util
import org.gradle.api.GradleException
import org.gradle.api.Project

class SuggestMergedModuleInfoTaskImpl extends BaseTaskImpl<SuggestMergedModuleInfoTaskData> {
    SuggestMergedModuleInfoTaskImpl(Project project, SuggestMergedModuleInfoTaskData taskData) {
        super(project, taskData)
        project.logger.info("taskData: $taskData")
    }

    void execute() {
        project.logger.info("Executing suggestMergedModuleInfo with useJdeps = $td.useJdeps")
        if(td.useJdeps != JdepsUsage.no) {
            try {
                def jarFilePath = "$td.jlinkBasePath/suggestedMergedModule.jar"
                new File(jarFilePath).delete()
                Util.createJar(project, td.javaHome, jarFilePath, td.mergedJarsDir)
                def result = new JdepsExecutor(project).genModuleInfo(project.file(jarFilePath),
                        project.file(td.tmpJarsDirPath), td.jlinkJarsDirPath, td.javaHome)
                def loggerFun = result.exitValue ? (td.useJdeps == JdepsUsage.yes) ? 'warn' : 'error' : 'info'
                project.logger."$loggerFun"(result.output)
                if(result.exitValue) {
                    if(td.useJdeps == JdepsUsage.exclusively) {
                        throw new GradleException("jdeps exited with return code $result.exitValue")
                    }
                } else {
                    println "jdeps generated module-info.java:\n${result.moduleInfoFile?.text}"
                    return
                }
            } catch(Exception e) {
                if(td.useJdeps == JdepsUsage.exclusively) {
                    throw new GradleException("jdeps failed", e)
                }
                if(project.logger.infoEnabled) {
                    project.logger.info("jdeps failed.", e)
                } else {
                    project.logger.warn("jdeps failed: $e")
                }
            }
            if(td.useJdeps == JdepsUsage.exclusively) return
        }
        def builder = new SuggestedMergedModuleInfoBuilder(project, td.mergedJarsDir, td.javaHome, td.forceMergedJarPrefixes, td.extraDependenciesPrefixes)
        def blockStart = (td.language == ModuleInfo.Language.KOTLIN) ?
                'mergedModule (delegateClosureOf<ModuleInfo> {' : 'mergedModule {'
        def blockEnd = (td.language == ModuleInfo.Language.KOTLIN) ? '})' : '}'
        println "$blockStart\n${builder.moduleInfo.toString(4, td.language)}\n$blockEnd"
    }
}

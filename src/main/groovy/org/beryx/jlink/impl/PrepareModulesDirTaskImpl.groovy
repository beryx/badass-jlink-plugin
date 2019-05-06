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
import org.beryx.jlink.data.PrepareModulesDirTaskData
import org.beryx.jlink.util.DependencyManager
import org.beryx.jlink.util.ModuleInfoAdjuster
import org.beryx.jlink.util.Util
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class PrepareModulesDirTaskImpl extends BaseTaskImpl<PrepareModulesDirTaskData> {
    private static final Logger LOGGER = Logging.getLogger(PrepareModulesDirTaskImpl.class);

    PrepareModulesDirTaskImpl(Project project, PrepareModulesDirTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        LOGGER.info("Copying delegating modules to ${td.jlinkJarsDir}...")
        project.copy {
            into td.jlinkJarsDir
            from td.delegatingModulesDir
        }

        LOGGER.info("Copying modular jars not required by non-modular jars to ${td.jlinkJarsDir}...")
        def depMgr = new DependencyManager(project, td.forceMergedJarPrefixes, td.extraDependenciesPrefixes)
        project.copy {
            into td.jlinkJarsDir
            from (depMgr.modularJars - depMgr.modularJarsRequiredByNonModularJars)
        }

        project.copy {
            into td.jlinkJarsDir
            from (project.jar.archivePath)
        }

        adjustModuleDescriptors(depMgr)

        def mainModuleJar = new File(td.jlinkJarsDir, project.jar.archivePath.name)
        def targetPath = "$td.jlinkJarsDir/${td.moduleName}.jar"
    }

    @CompileDynamic
    private void adjustModuleDescriptors(DependencyManager depMgr) {
        def nonModularModules = depMgr.nonModularJars.collect { Util.getModuleName(it) }
        def adjuster = new ModuleInfoAdjuster(td.mergedModuleName, nonModularModules)
        def jarMap = (depMgr.modularJars + project.jar.archivePath).collectEntries { [it.name, it] }
        td.jlinkJarsDir.listFiles().each { File jar ->
            if(jarMap.keySet().contains(jar.name)) {
                def adjustedDescriptors = adjuster.getAdjustedDescriptors(jarMap[jar.name])
                adjustedDescriptors.each { moduleInfoPath, descriptorBytes ->
                    project.delete(td.tmpModuleInfoDirPath)
                    def moduleInfoFile = new File("$td.tmpModuleInfoDirPath/$moduleInfoPath")
                    project.mkdir(moduleInfoFile.parent)
                    moduleInfoFile.withOutputStream { stream ->
                        stream << descriptorBytes
                    }
                    project.exec {
                        commandLine "$td.javaHome/bin/jar",
                                '--update',
                                '--file',
                                jar.path,
                                '-C',
                                td.tmpModuleInfoDirPath,
                                moduleInfoPath
                    }
                }
            }
        }
    }
}

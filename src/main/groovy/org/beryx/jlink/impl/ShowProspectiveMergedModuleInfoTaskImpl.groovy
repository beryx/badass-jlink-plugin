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

import org.beryx.jlink.data.ShowProspectiveMergedModuleInfoTaskData
import org.beryx.jlink.util.ModuleManager
import org.beryx.jlink.util.PackageUseScanner
import org.gradle.api.Project

class ShowProspectiveMergedModuleInfoTaskImpl extends BaseTaskImpl<ShowProspectiveMergedModuleInfoTaskData> {
    ShowProspectiveMergedModuleInfoTaskImpl(Project project, ShowProspectiveMergedModuleInfoTaskData taskData) {
        super(project, taskData)
        project.logger.info("taskData: $taskData")
    }

    void execute() {
        def scanner = new PackageUseScanner(project)
        def invalidFiles = scanner.scan(td.mergedJarsDir)
        if(invalidFiles) {
            project.logger.warn("Failed to scan: $invalidFiles")
        }
        project.logger.debug("External packages used by the merged module:\n\t${scanner.externalPackages.join('\n\t')}")

        def depMgr = new DependencyManager(project, td.forceMergedJarPrefixes)
        def moduleManager = new ModuleManager(*depMgr.modularJars.toArray(), new File("$td.javaHome/jmods"))
        def requiredModules = new TreeSet<String>()

        scanner.externalPackages.each { pkg ->
            def moduleName = moduleManager.exportMap[pkg]
            if(!moduleName) {
                project.logger.info("Cannot find module exporting $pkg")
            } else {
                requiredModules.add(moduleName)
            }
        }
        requiredModules -= 'java.base'

        println "mergedModule {"

        requiredModules.each {println "\trequires '$it'"}

        println "}\n"

    }
}

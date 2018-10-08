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
package org.beryx.jlink.util

import groovy.transform.TupleConstructor
import org.beryx.jlink.data.ModuleInfo
import org.gradle.api.Project

import static org.beryx.jlink.data.ModuleInfo.*

@TupleConstructor
class ProspectiveMergedModuleInfoBuilder {
    final Project project;
    final File mergedJarsDir;
    String javaHome
    final List<String> forceMergedJarPrefixes

    ModuleInfo getModuleInfo() {
        def info = new ModuleInfo()
        info.requiresBuilders.addAll(requiresBuilders)
        info.usesBuilders.addAll(usesBuilders)
        info.providesBuilders.addAll(providesBuilders)
        info
    }

    Set<UsesBuilder> getUsesBuilders() {
        def scanner = new ServiceLoaderUseScanner(project)
        scanner.scan(mergedJarsDir)
        scanner.builders
    }

    Set<ProvidesBuilder> getProvidesBuilders() {
        def scanner = new ServiceProviderScanner(project)
        scanner.scan(mergedJarsDir)
        scanner.builders
    }

    Set<RequiresBuilder> getRequiresBuilders() {
        def scanner = new PackageUseScanner(project)
        def invalidFiles = scanner.scan(mergedJarsDir)
        if(invalidFiles) {
            project.logger.warn("Failed to scan: $invalidFiles")
        }
        project.logger.debug("External packages used by the merged service:\n\t${scanner.externalPackages.join('\n\t')}")

        def depMgr = new DependencyManager(project, forceMergedJarPrefixes)
        def moduleManager = new ModuleManager(*depMgr.modularJars.toArray(), new File("$javaHome/jmods"))
        def builders = new HashSet<RequiresBuilder>()

        scanner.externalPackages.each { pkg ->
            def moduleName = moduleManager.exportMap[pkg]
            if(!moduleName) {
                project.logger.info("Cannot find service exporting $pkg")
            } else if(moduleName != 'java.base'){
                builders << new RequiresBuilder(moduleName)
            }
        }
        builders
    }
}

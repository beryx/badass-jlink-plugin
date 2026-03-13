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
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class PrepareModulesDirTaskImpl extends BaseTaskImpl<PrepareModulesDirTaskData> {
    private static final Logger LOGGER = Logging.getLogger(PrepareModulesDirTaskImpl.class);

    final FileSystemOperations fileSystemOperations
    final ArchiveOperations archiveOperations

    PrepareModulesDirTaskImpl(FileSystemOperations fileSystemOperations, ArchiveOperations archiveOperations, PrepareModulesDirTaskData taskData) {
        super(taskData)
        this.fileSystemOperations = fileSystemOperations
        this.archiveOperations = archiveOperations
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        LOGGER.info("Copying delegating modules to ${td.jlinkJarsDir}...")
        fileSystemOperations.copy {
            into td.jlinkJarsDir
            from td.delegatingModulesDir
        }

        LOGGER.info("Copying modular jars not required by non-modular jars to ${td.jlinkJarsDir}...")
        def depMgr = new DependencyManager( td.forceMergedJarPrefixes, td.extraDependenciesPrefixes, td.dependencyData)
        fileSystemOperations.copy {
            into td.jlinkJarsDir
            from (depMgr.modularJars - depMgr.modularJarsRequiredByNonModularJars)
        }

        fileSystemOperations.copy {
            from td.projectArchiveFile
            into td.jlinkJarsDir
        }

        adjustModuleDescriptors(depMgr)
    }

    @CompileDynamic
    private void adjustModuleDescriptors(DependencyManager depMgr) {
        def nonModularModules = depMgr.nonModularJars.collect { Util.getModuleName(it) }
        def adjuster = new ModuleInfoAdjuster(td.mergedModuleName, nonModularModules)
        def jarMap = (depMgr.modularJars + td.projectArchiveFile).collectEntries { [it.name, it] }
        td.jlinkJarsDir.listFiles().each { File jar ->
            if(jarMap.keySet().contains(jar.name)) {
                def adjustedDescriptors = adjuster.getAdjustedDescriptors(jarMap[jar.name])
                adjustedDescriptors.each { moduleInfoPath, descriptorBytes ->
                    fileSystemOperations.delete { it.delete(td.tmpModuleInfoDirPath) }
                    def moduleInfoFile = new File("$td.tmpModuleInfoDirPath/$moduleInfoPath")
                    moduleInfoFile.parentFile.mkdirs()
                    fileSystemOperations.copy {
                        from archiveOperations.zipTree(jar.path)
                        into td.tmpModuleInfoDirPath
                    }
                    moduleInfoFile.withOutputStream { stream ->
                        stream << descriptorBytes
                    }
                    fileSystemOperations.delete { it.delete(jar) }
                    Util.createJar(jar, new File(td.tmpModuleInfoDirPath))
                }
            }
        }
    }
}

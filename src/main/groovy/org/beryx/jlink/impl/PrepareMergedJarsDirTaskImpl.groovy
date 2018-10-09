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

import org.beryx.jlink.data.PrepareMergedJarsDirTaskData
import org.beryx.jlink.util.DependencyManager
import org.beryx.jlink.util.Util
import org.gradle.api.Project

class PrepareMergedJarsDirTaskImpl extends BaseTaskImpl<PrepareMergedJarsDirTaskData> {
    PrepareMergedJarsDirTaskImpl(Project project, PrepareMergedJarsDirTaskData taskData) {
        super(project, taskData)
        project.logger.info("taskData: $taskData")
    }

    void execute() {
        project.delete(td.jlinkBasePath)
        td.mergedJarsDir.mkdirs()
        def depMgr = new DependencyManager(project, td.forceMergedJarPrefixes, td.extraDependenciesPrefixes)
        copyRuntimeJars(depMgr)
        mergeUnpackedContents(new File(td.nonModularJarsDirPath).listFiles() as List)
    }

    def copyRuntimeJars(DependencyManager depMgr) {
        project.delete(td.jlinkJarsDirPath, td.nonModularJarsDirPath)
        new File(td.jlinkJarsDirPath).mkdirs()
        new File(td.nonModularJarsDirPath).mkdirs()
        project.logger.info("Copying modular jars required by non-modular jars to ${td.jlinkJarsDirPath}...")
        depMgr.modularJarsRequiredByNonModularJars.each { jar ->
            project.logger.debug("\t... from $jar ...")
            project.copy {
                into td.jlinkJarsDirPath
                from jar
            }
        }
        project.logger.info("Copying mon-modular jars to ${td.nonModularJarsDirPath}...")
        depMgr.nonModularJars.each { jar ->
            project.copy {
                into td.nonModularJarsDirPath
                from jar
            }
        }
    }

    def mergeUnpackedContents(Collection<File> jars) {
        if(jars.empty) return
        project.logger.info("Merging content into ${td.mergedJarsDir}...")

        jars.each { jar ->
            project.delete(td.tmpJarsDirPath)
            project.copy {
                from project.zipTree(jar)
                into td.tmpJarsDirPath
                exclude 'module-info.class'
            }
            def versionedDir = Util.getVersionedDir(new File(td.tmpJarsDirPath), td.jvmVersion)
            if(versionedDir?.directory) {
                project.copy {
                    from versionedDir
                    into td.tmpJarsDirPath
                }
            }

            project.delete("$td.tmpJarsDirPath/META-INF/versions")

            project.ant.move file: td.tmpJarsDirPath, tofile: td.mergedJarsDir
        }
        Util.createManifest(td.mergedJarsDir, false)
    }
}

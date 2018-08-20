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


import org.gradle.api.Project

class BaseTaskImpl {
    static String SEP = File.pathSeparatorChar

    final Project project

    final String jlinkBasePath
    final String nonModularJarsDirPath
    final String mergedJarsDirPath
    final String tmpMergedModuleDirPath
    final String jlinkJarsDirPath
    final String tmpJarsDirPath
    final String tmpModuleInfoDirPath

    BaseTaskImpl(Project project) {
        this.project = project

        jlinkBasePath = "$project.buildDir/jlinkbase"
        nonModularJarsDirPath = "$jlinkBasePath/nonmodjars"
        mergedJarsDirPath = "$jlinkBasePath/mergedjars"
        tmpMergedModuleDirPath = "$jlinkBasePath/tmpmerged"
        jlinkJarsDirPath = "$jlinkBasePath/jlinkjars"
        tmpJarsDirPath = "$jlinkBasePath/tmpjars"
        tmpModuleInfoDirPath = "$jlinkBasePath/tmpmodinfo"
    }
}

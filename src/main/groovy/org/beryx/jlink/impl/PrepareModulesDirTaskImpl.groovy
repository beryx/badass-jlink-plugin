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

import org.beryx.jlink.taskdata.PrepareModulesDirTaskData
import org.gradle.api.Project

class PrepareModulesDirTaskImpl extends BaseTaskImpl {
    final List<String> forceMergedJarPrefixes

    PrepareModulesDirTaskImpl(Project project, PrepareModulesDirTaskData taskData) {
        super(project)

        this.forceMergedJarPrefixes = taskData.forceMergedJarPrefixes
    }

    void execute() {
        project.logger.info("Copying modular jars not required by non-modular jars to ${jlinkJarsDirPath}...")
        def depMgr = new DependencyManager(project, forceMergedJarPrefixes)
        project.copy {
            into jlinkJarsDirPath
            from (depMgr.modularJars - depMgr.modularJarsRequiredByNonModularJars)
        }
    }
}

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

import org.beryx.jlink.data.JlinkTaskData
import org.gradle.api.Project

class JlinkTaskImpl extends BaseTaskImpl<JlinkTaskData> {
    JlinkTaskImpl(Project project, JlinkTaskData taskData) {
        super(project, taskData)
    }

    void execute() {
        project.delete(td.imageDir)
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jlinkOutput = {
                return standardOutput.toString()
            }
            commandLine = ["$td.javaHome/bin/jlink",
                           '-v',
                           *td.options,
                           '--module-path',
                           "$td.javaHome/jmods/$SEP${project.files(td.jlinkJarsDir).asPath}$SEP${project.jar.archivePath}",
                           '--add-modules', td.moduleName,
                           '--output', td.imageDir,
                           '--launcher', "$td.launcherName=$td.moduleName/$td.mainClass"]
        }
        if(result.exitValue != 0) {
            project.logger.error(project.ext.jlinkOutput())
        } else {
            project.logger.info(project.ext.jlinkOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }
}

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

import org.beryx.jlink.taskdata.JlinkZipTaskData
import org.gradle.api.Project

class JlinkZipTaskImpl extends BaseTaskImpl {
    final File imageDir
    final File imageZip

    JlinkZipTaskImpl(Project project, JlinkZipTaskData taskData) {
        super(project)
        this.imageDir = taskData.imageDir
        this.imageZip = taskData.imageZip
    }

    void execute() {
        project.ant.zip(destfile: imageZip, duplicate: 'fail') {
            zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/**", excludes: "$imageDir.name/bin/**")
            zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/bin/**", filemode: 755)
        }
    }
}

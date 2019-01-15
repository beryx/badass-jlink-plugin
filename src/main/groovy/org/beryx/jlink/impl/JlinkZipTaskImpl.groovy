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
import org.beryx.jlink.data.JlinkZipTaskData
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class JlinkZipTaskImpl extends BaseTaskImpl<JlinkZipTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JlinkZipTaskImpl.class);

    JlinkZipTaskImpl(Project project, JlinkZipTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        if(td.targetPlatforms) {
            def zipDir = td.imageZip.parentFile
            def zipName = td.imageZip.name
            int pos = zipName.lastIndexOf('.')
            def ext = (pos > 0) ? zipName.substring(pos+1) : 'zip'
            def baseName = (pos > 0) ? zipName.substring(0,pos) : zipName
            td.targetPlatforms.values().each { platform ->
                File zipFile = new File(zipDir, "${baseName}-${platform.name}.${ext}")
                File imageDir = new File(td.imageDir, "$td.launcherData.name-$platform.name")
                project.ant.zip(destfile: zipFile, duplicate: 'fail') {
                    zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/**", excludes: "$imageDir.name/bin/**")
                    zipfileset(dir: imageDir.parentFile, includes: "$imageDir.name/bin/**", filemode: 755)
                }
            }
        } else {
            project.ant.zip(destfile: td.imageZip, duplicate: 'fail') {
                zipfileset(dir: td.imageDir.parentFile, includes: "$td.imageDir.name/**", excludes: "$td.imageDir.name/bin/**")
                zipfileset(dir: td.imageDir.parentFile, includes: "$td.imageDir.name/bin/**", filemode: 755)
            }
        }
    }
}

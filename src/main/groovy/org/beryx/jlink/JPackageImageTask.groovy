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
package org.beryx.jlink

import groovy.transform.CompileStatic
import org.beryx.jlink.data.CustomImageData
import org.beryx.jlink.impl.JPackageImageTaskImpl
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class JPackageImageTask extends AbstractJPackageTask {
    @Input
    CustomImageData getCustomImageData() {
        extension.customImageData.get()
    }

    @OutputDirectory
    File getImageOutputDir() {
        extension.jpackageData.get().getImageOutputDir()
    }

    JPackageImageTask() {
        dependsOn(JlinkPlugin.TASK_NAME_JLINK)
        description = 'Creates an installable image using the jpackage tool'
    }

    @TaskAction
    void jpackageTaskAction() {
        def taskData = createTaskData()
        taskData.customImageData = customImageData

        def taskImpl = new JPackageImageTaskImpl(fileSystemOperations, execOperations, taskData)
        taskImpl.execute()
    }
}

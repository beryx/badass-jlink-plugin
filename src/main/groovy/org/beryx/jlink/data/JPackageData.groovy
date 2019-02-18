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
package org.beryx.jlink.data

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@CompileStatic
@ToString(includeNames = true)
class JPackageData {
    private final Project project
    private final LauncherData launcherData

    @Input
    String jpackageHome

    @Input
    String outputDir = 'jpackage'

    File imageOutputDir

    String imageName

    @Input
    List<String> imageOptions = []

    @Input
    boolean skipInstaller = false

    @Input @Optional
    String installerType

    File installerOutputDir

    String installerName

    @Input
    List<String> installerOptions = []

    List<String> jvmArgs = []

    JPackageData(Project project, LauncherData launcherData) {
        this.project = project
        this.launcherData = launcherData
        this.imageName = project.name
        this.installerName = project.name
        this.jpackageHome = defaultJPackageHome
    }

    @Input
    String getImageName() {
        this.@imageName ?: launcherData.name
    }

    @Input
    String getInstallerName() {
        this.@installerName ?: launcherData.name
    }

    @Input
    List<String> getJvmArgs() {
        this.@jvmArgs ?: launcherData.jvmArgs
    }

    @OutputDirectory
    File getImageOutputDir() {
        this.@imageOutputDir ?: project.file("$project.buildDir/$outputDir")
    }

    @OutputDirectory
    File getInstallerOutputDir() {
        this.@installerOutputDir ?: project.file("$project.buildDir/$outputDir")
    }


    private static String getDefaultJPackageHome() {
        def value = System.properties['badass.jlink.jpackage.home']
        if(value) return value
        value = System.getenv('BADASS_JLINK_JPACKAGE_HOME')
        if(value) return value
        value = System.properties['java.home']
        String ext = System.getProperty('os.name', '').toLowerCase().contains('win') ? '.exe' : ''
        if(new File("$value/bin/jpackage$ext").file) return value
        return System.getenv('JAVA_HOME')
    }
}

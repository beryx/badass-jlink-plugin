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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
@ToString(includeNames = true)
class JPackageData {
    private final Project project
    private final LauncherData launcherData
    private final List<SecondaryLauncherData> secondaryLaunchers = []

    @Input
    String jpackageHome

    @Input
    String outputDir = 'jpackage'

    File imageOutputDir

    String imageName

    @Input
    List<String> imageOptions = []

    @InputDirectory @Optional
    File resourceDir

    @Input @Optional
    String targetPlatformName

    @Input
    boolean skipInstaller = false

    @Input @Optional
    String installerType

    File installerOutputDir

    String installerName

    @Input @Optional
    String appVersion

    @Input @Optional
    String icon

    @Input
    List<String> installerOptions = []

    List<String> args = []

    List<String> jvmArgs = []

    JPackageData(Project project, LauncherData launcherData) {
        this.project = project
        this.launcherData = launcherData
        this.jpackageHome = defaultJPackageHome
    }

    @Input
    String getImageName() {
        this.@imageName ?: launcherData.name ?: project.name
    }

    @Input
    String getInstallerName() {
        this.@installerName ?: launcherData.name ?: project.name
    }

    @Input
    String getLauncherName() {
        launcherData.name
    }

    @Input
    List<SecondaryLauncherData> getSecondaryLaunchers() {
        this.@secondaryLaunchers
    }

    void addSecondaryLauncher(SecondaryLauncherData launcher) {
        secondaryLaunchers << launcher
    }

    @Input
    List<String> getJvmArgs() {
        this.@jvmArgs ?: launcherData.jvmArgs
    }

    @Input
    List<String> getArgs() {
        this.@args ?: launcherData.args
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
        if(new File("$value/bin/jpackage$EXEC_EXTENSION").file) return value
        return System.getenv('JAVA_HOME')
    }
}

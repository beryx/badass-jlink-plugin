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
import org.beryx.jlink.data.JPackageTaskData
import org.beryx.jlink.util.Util
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

@CompileStatic
class JPackageImageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTaskImpl.class);

    JPackageImageTaskImpl(Project project, JPackageTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jpackageImageOutput = {
                return standardOutput.toString()
            }
            def outputDir = td.jpackageData.imageOutputDir
            project.delete(outputDir)
            def jpd = td.jpackageData
            def jpackageExec = "$jpd.jpackageHome/bin/jpackage$EXEC_EXTENSION"
            Util.checkExecutable(jpackageExec)

            Map<String,File> propFiles = [:]
            jpd.secondaryLaunchers.each { launcher ->
                def propFile = new File("$td.jlinkBasePath/${launcher.name}.propeties")
                Files.deleteIfExists(propFile.toPath())
                propFile.withOutputStream { stream ->
                    if(launcher.moduleName) {
                        stream << "module=$launcher.moduleName\n"
                    }
                    if(launcher.mainClass) {
                        stream << "main-class=$launcher.mainClass\n"
                    }
                    if(launcher.args) {
                        stream << "arguments=${launcher.args.collect{adjustArg(it)}.join('\\n')}\n"
                    }
                    if(launcher.jvmArgs) {
                        stream << "java-options=${launcher.jvmArgs.collect{adjustArg(it)}.join('\\n')}\n"
                    }
                }
                propFiles[launcher.name] = propFile
            }

            def appVersion = (jpd.appVersion ?: project.version).toString()
            def versionOpts = (appVersion == Project.DEFAULT_VERSION) ? [] : [ '--app-version', appVersion ]
            if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                throw new GradleException("The first character of the --app-version argument should be a digit.")
            }

            final def resourceDir = jpd.getResourceDir()
            final def resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir ]

            commandLine = [jpackageExec,
                           '--type', 'app-image',
                           '--dest', outputDir,
                           '--name', jpd.imageName,
                           '--module', "$td.moduleName/$td.mainClass",
                           *versionOpts,
                           '--runtime-image', td.runtimeImageDir,
                           *resourceOpts,
                           *(jpd.jvmArgs ? jpd.jvmArgs.collect{['--java-options', adjustArg(it)]}.flatten() : []),
                           *(jpd.args ? jpd.args.collect{['--arguments', adjustArg(it)]}.flatten() : []),
                           *(propFiles ? propFiles.collect{['--add-launcher', it.key + '=' +  it.value.absolutePath]}.flatten() : []),
                           *jpd.imageOptions]
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jpackageImageOutput())
        } else {
            LOGGER.info(project.ext.jpackageImageOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    static String adjustArg(String arg) {
        def adjusted = arg.replace('"', '\\"')
        if(!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8227641
        adjusted = adjusted.replace(' ', '\\" \\"')
        File.separator
        adjusted = adjusted.replace('{{BIN_DIR}}', '$APPDIR' + File.separator + '..')
        adjusted
    }
}

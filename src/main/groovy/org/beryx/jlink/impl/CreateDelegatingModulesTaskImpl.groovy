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
import org.beryx.jlink.util.Util
import org.beryx.jlink.data.CreateDelegatingModulesTaskData
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class CreateDelegatingModulesTaskImpl {
    private static final Logger LOGGER = Logging.getLogger(CreateDelegatingModulesTaskImpl.class);

    final CreateDelegatingModulesTaskData td

    CreateDelegatingModulesTaskImpl(CreateDelegatingModulesTaskData taskData) {
        this.td = taskData
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        LOGGER.info("Creating delegating modules...")
        td.fileSystemOperations.delete { spec ->
            spec.delete(td.tmpJarsDir)
        }

        td.nonModularJarsDir.eachFile { jarFile ->
            createDelegatingModule(jarFile, td.tmpJarsDir, td.delegatingModulesDir)
        }
    }

    @CompileDynamic
    def createDelegatingModule(File jarFile, File tmpDir, File targetDir) {
        def moduleDir = genDelegatingModuleInfo(jarFile, tmpDir)
        if(!moduleDir) return
        td.fileSystemOperations.delete { spec ->
            spec.delete(td.tmpModuleInfoDir)
        }
        Util.createManifest(td.tmpModuleInfoDir, false)
        LOGGER.info("Compiling delegating module $moduleDir.name ...")
        def result = {
            def execOps = td.execOperations

            def outputStream = new ByteArrayOutputStream()

            def execResult = execOps.exec { spec ->
                spec.ignoreExitValue = true
                spec.standardOutput = outputStream
                spec.commandLine = [
                        "$td.javaHome/bin/javac",
                        '-p',
                        td.jlinkJarsDir.path,
                        '-d',
                        td.tmpModuleInfoDir.path,
                        "${moduleDir.path}/module-info.java"
                ]
            }

            def javacOutput = outputStream.toString()

            if(execResult.exitValue != 0) {
                LOGGER.error(javacOutput)
            } else {
                LOGGER.info(javacOutput)
            }
            execResult.assertNormalExitValue()
            execResult.rethrowFailure()
            return execResult
        }()

        def targetJarPath = new File(targetDir, jarFile.name)
        Util.createJar(targetJarPath, td.tmpModuleInfoDir)
    }

    File genDelegatingModuleInfo(File jarFile, File targetDir) {
        def moduleName = Util.getModuleName(jarFile)
        def modinfoDir = new File(targetDir, moduleName)
        modinfoDir.mkdirs()
        def modInfoJava = new File(modinfoDir, 'module-info.java')
        if(modInfoJava.exists()) {
            LOGGER.info("Module $moduleName already generated. Skipping $jarFile")
            return null
        }
        LOGGER.info("Generating module $moduleName for: $jarFile")
        modInfoJava << """
            open module $moduleName {
                requires transitive $td.mergedModuleName;
            }
            """.stripIndent()
        modinfoDir
    }
}

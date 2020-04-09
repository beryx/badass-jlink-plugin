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
package org.beryx.jlink.util

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.beryx.jlink.data.LauncherData
import org.gradle.api.GradleException

import java.util.function.Function

@CompileStatic
@TupleConstructor
class LaunchScriptGenerator {
    final String moduleName
    final String mainClassName
    final LauncherData launcherData

    enum Type {
        UNIX(
                '',
                {LauncherData ld -> ld.unixScriptTemplate},
                'unixScriptTemplate.txt',
                '$DIR'),
        WINDOWS(
                '.bat',
                {LauncherData ld -> ld.windowsScriptTemplate},
                'windowsScriptTemplate.txt',
                '%~dp0')

        final String extension
        final Function<LauncherData, File> templateProvider
        final String defaultTemplate
        final String binDirPlaceholder

        Type(String extension, Function<LauncherData, File> templateProvider, String defaultTemplate, String binDirPlaceholder) {
            this.extension = extension
            this.templateProvider = templateProvider
            this.defaultTemplate = defaultTemplate
            this.binDirPlaceholder = binDirPlaceholder
        }
    }

    void generate(String targetDirPath) {
        new File(targetDirPath).mkdirs()
        for(type in Type.values()) {
            def scriptText = getScript(type)
            def scriptFile = new File(targetDirPath, "${launcherData.name}${type.extension}")
            scriptFile << scriptText
            scriptFile.setExecutable(true, false)
        }
    }

    @CompileDynamic
    String getScript(Type type) {
        def engine = new SimpleTemplateEngine()
        def args = launcherData.args.collect{adjustArg(it, type)}.join(' ')
        def jvmArgs = launcherData.jvmArgs.collect{adjustArg(it, type)}.join(' ')
        def bindings = [
                moduleName: moduleName,
                mainClassName: mainClassName,
                args: args,
                jvmArgs: jvmArgs,
        ]
        File templateFile = type.templateProvider.apply(launcherData)
        Template template
        if(templateFile) {
            if(!templateFile.file) {
                throw new GradleException("Template file $templateFile not found.")
            }
            template = engine.createTemplate(templateFile)
        } else {
            def templateURL = LaunchScriptGenerator.class.getResource("/$type.defaultTemplate")
            if(!templateURL) {
                throw new GradleException("Template resource $templateFile not found.")
            }
            template = engine.createTemplate(templateURL)
        }
        template.make(bindings).toString()
    }

    static String adjustArg(String arg, Type type) {
        def adjusted = arg.replace('"', '\\"')
        if(!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        adjusted.replace('{{BIN_DIR}}', type.binDirPlaceholder)
    }
}

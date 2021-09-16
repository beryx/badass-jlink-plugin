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
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.beryx.jlink.data.LauncherData
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@TupleConstructor
class LaunchScriptGenerator {
    final Project project
    final String moduleName
    final String mainClassName
    final LauncherData launcherData

    enum Type {
        UNIX(
                '',
                {LauncherData ld -> ld.unixScriptTemplate},
                'unixScriptTemplate.txt',
                'unixScriptTemplate.txt',
                '$DIR',
                '$HOME',
                '\\$$1'),
        WINDOWS(
                '.bat',
                {LauncherData ld -> ld.windowsScriptTemplate},
                'windowsScriptTemplate.txt',
                'windowsScriptTemplateJavaw.txt',
                '%~dp0',
                '%USERPROFILE%',
                '%$1%')

        final String extension
        final Function<LauncherData, File> templateProvider
        final String defaultTemplate
        final String defaultTemplateNoConsole
        final String binDirPlaceholder
        final String homeDirPlaceholder
        final String envVarReplacement

        Type(String extension, Function<LauncherData, File> templateProvider,
             String defaultTemplate, String defaultTemplateNoConsole,
             String binDirPlaceholder, String homeDirPlaceholder,
             String envVarReplacement) {
            this.extension = extension
            this.templateProvider = templateProvider
            this.defaultTemplate = defaultTemplate
            this.defaultTemplateNoConsole = defaultTemplateNoConsole
            this.binDirPlaceholder = binDirPlaceholder
            this.homeDirPlaceholder = homeDirPlaceholder
            this.envVarReplacement = envVarReplacement
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

    String getScript(Type type) {
        def engine = new SimpleTemplateEngine()

        def args = launcherData.getArgs(project).stream()
                .map{adjustArg(it, type) as CharSequence}
                .collect(Collectors.joining(' '))

        def jvmArgs = launcherData.getJvmArgs(project).stream()
                .map{adjustArg(it, type) as CharSequence}
                .collect(Collectors.joining(' '))

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
            def templateName = launcherData.noConsole ? type.defaultTemplateNoConsole : type.defaultTemplate
            def templateURL = LaunchScriptGenerator.class.getResource("/$templateName")
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
        adjusted = adjusted.replace('{{BIN_DIR}}', type.binDirPlaceholder)
        adjusted = adjusted.replace('{{HOME_DIR}}', type.homeDirPlaceholder)
        adjusted = adjusted.replaceAll(/\{\{([\w.]+)}}/, type.envVarReplacement)
        return adjusted
    }
}

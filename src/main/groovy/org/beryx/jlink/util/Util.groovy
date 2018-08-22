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

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.lang.module.ModuleFinder
import java.util.jar.JarFile

class Util {
    static String toModuleName(String s) {
        def name = s.replaceAll('[^0-9A-Za-z_.]', '.')
        int start = 0
        while(name[start] == '.') start++
        int end = name.length() - 1
        while(name[end] == '.') end--
        name = name.substring(start, end + 1)
        name.replaceAll('\\.[.]+', '.')
    }

    static String getModuleNameFrom(String moduleInfoText) {
        def matcher = moduleInfoText.readLines().collect {it =~ /\s*module\s+(\S+)\s*\{.*/}.find {it.matches()}
        if(matcher == null) throw new GradleException("Cannot retrieve module name from module-info.java")
        matcher[0][1]
    }

    static String getDefaultMergedModuleName(Project project) {
        String name = (project.group ?: project.name) as String
        "${Util.toModuleName(name)}.merged.module"
    }

    static String getDefaultModuleName(Project project) {
        Set<File> srcDirs = project.sourceSets.main.java.srcDirs
        File moduleInfoDir = srcDirs.find { it.list().contains('module-info.java')}
        if(!moduleInfoDir) throw new GradleException("Cannot find module-info.java")
        String moduleInfoText = new File(moduleInfoDir, 'module-info.java').text
        Util.getModuleNameFrom(moduleInfoText)
    }

    static String getPackage(String entryName) {
        if(!entryName.endsWith('.class')) return null;
        int pos = entryName.lastIndexOf('/')
        if(pos <= 0) return null
        def pkgName = entryName.substring(0, pos).replace('/', '.')
        boolean valid = pkgName.split('\\.').every {isValidIdentifier(it)}
        return valid ? pkgName : null
    }

    static boolean isValidIdentifier(String name) {
        if (!name) return false
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false
        }
        true
    }

    static String getModuleName(File f, Project project) {
        try {
            return ModuleFinder.of(f.toPath()).findAll().first().descriptor().name()
        } catch (Exception e) {
            def modName = getFallbackModuleName(f)
            project.logger.warn("Cannot retrieve the module name of $f. Using fallback value: $modName.", e)
            return modName
        }
    }

    static String getFallbackModuleName(File f) {
        def modName = new JarFile(f).getManifest()?.mainAttributes.getValue('Automatic-Module-Name')
        if(modName) return modName
        def s = f.name
        def tokens = s.split('-[0-9]')
        if(tokens.length < 2) return s - '.jar'
        def len = s.length()
        return s.substring(0, len - tokens[-1].length() - 2).replace('-', '.')
    }

    static void createManifest(String targetDirPath) {
        def mfdir = new File(targetDirPath, 'META-INF')
        mfdir.mkdirs()
        def mf = new File(mfdir, 'MANIFEST.MF')
        mf.delete()
        mf << """
        Manifest-Version: 1.0
        Created-By: Badass-JLink Plugin
        Built-By: gradle
        """.stripMargin()
    }

    static void createJar(Project project, String javaHome, String jarFilePath, String contentDirPath) {
        project.file(jarFilePath).parentFile.mkdirs()
        project.exec {
            commandLine "$javaHome/bin/jar",
                    '--create',
                    '--file',
                    jarFilePath,
                    '-C',
                    contentDirPath,
                    '.'
        }
    }

}

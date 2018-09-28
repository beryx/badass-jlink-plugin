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

import java.lang.module.ModuleDescriptor
import java.util.zip.ZipFile

class ModuleManager {
    // [module name : descriptor]
    final Map<String, ModuleDescriptor> moduleMap = [:]

    // [package : module name]
    final Map<String, String> exportMap = [:]

    ModuleManager(Object... modulePath) {
        getJarsAndMods(modulePath).each {
            def md = getModuleDescriptor(it)
            if(md) {
                moduleMap[md.name()] = md
                md.exports().each {exportMap[it.source()] = md.name()}
            }
        }
    }

    private static List<File> getJarsAndMods(Object... modulePath) {
        List<File> allFiles = []
        modulePath.each {entry ->
            File f = (entry instanceof File) ? entry : new File(entry.toString())
            if(f.file) allFiles.add(f)
            if(f.directory) {
                allFiles.addAll(f.listFiles({it.file} as FileFilter))
            }
        }
        allFiles.findAll {it.name.endsWith(".jar") || it.name.endsWith(".jmod")}
    }

    static ModuleDescriptor getModuleDescriptor(File f) {
        if(!f.file) throw new IllegalArgumentException("$f is not a file")
        if(f.name == 'module-info.class') return ModuleDescriptor.read(f.newInputStream())
        if(!f.name.endsWith('.jar') && !f.name.endsWith('.jmod')) throw new IllegalArgumentException("Unsupported file type: $f")
        def prefix = f.name.endsWith('.jmod') ? 'classes/' : ''
        def zipFile = new ZipFile(f)
        for(entry in zipFile.entries()) {
            if(entry.name == "${prefix}module-info.class" as String) {
                def entryStream = zipFile.getInputStream(entry)
                return ModuleDescriptor.read(entryStream)
            }
        }
        null
    }
}

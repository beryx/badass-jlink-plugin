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

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class ModuleManager {
    private static final Logger LOGGER = Logging.getLogger(ModuleManager.class);

    final String javaHome

    ModuleManager(String javaHome) {
        this.javaHome = javaHome
    }

    /**
     * @return map with entries of type [package : module-name]
     */
    Map<String, String> getExportsMap(String... modulePaths) {
        final Map<String, String> exportMap = [:]
        def runner = new SourceCodeRunner(javaHome, 'ModuleManager', sourceCode)
        String output = runner.getOutput(modulePaths);
        output.eachLine {line ->
            if(line) {
                def nameAndExports = line.split(':')
                if(nameAndExports.length > 1) {
                    String moduleName = nameAndExports[0]
                    for(String exportedPkg: nameAndExports[1].split(',')) {
                        exportMap[exportedPkg] = moduleName
                    }
                }
            }
        }
        exportMap
    }

    static final String sourceCode = """
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModuleManager {
    public static void main(String[] args) {
        List<String> exports = getExports(getJarsAndMods(args));
        System.out.println(exports.stream().collect(Collectors.joining("\\n")));
    }

    private static List<File> getJarsAndMods(String... modulePaths) {
        List<File> allFiles = new ArrayList<>();
        for(String path:  modulePaths) {
            File f = new File(path);
            if(f.isFile()) allFiles.add(f);
            if(f.isDirectory()) {
                allFiles.addAll(Arrays.asList(f.listFiles(File::isFile)));
            }
        }
        return allFiles.stream()
                .filter(f -> f.getName().endsWith(".jar") || f.getName().endsWith(".jmod"))
                .collect(Collectors.toList());
    }

    private static List<String> getExports(List<File> files) {
        List<String> exports = new ArrayList<>();
        for(File f: files) {
            ModuleDescriptor md = getModuleDescriptor(f);
            if(md != null && !md.exports().isEmpty()) {
                String exportedPackages = md.exports().stream()
                        .map(ModuleDescriptor.Exports::source)
                        .collect(Collectors.joining(","));
                exports.add(md.name() + ":" + exportedPackages);
            }
        }
        return exports;
    }

    $SourceCodeConstants.GET_MODULE_DESCRIPTOR
}
"""
}

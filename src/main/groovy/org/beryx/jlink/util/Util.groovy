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

import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.codehaus.groovy.tools.Utilities
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

import java.lang.module.ModuleFinder
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class Util {
    private static final Logger LOGGER = Logging.getLogger(Util.class);

    static String toModuleName(String s) {
        def name = s.replaceAll('[^0-9A-Za-z_.]', '.')
        int start = 0
        while(name[start] == '.') start++
        int end = name.length() - 1
        while(name[end] == '.') end--
        name = name.substring(start, end + 1)
        name.replaceAll('\\.[.]+', '.')
    }

    private static final Pattern MODULE_DECL = ~/(?m)(?s)\s*(?:open\s+)?module\s+(\S+)\s*\{.*/
    private static final Pattern COMMENT = ~"(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)"
    @CompileDynamic
    static String getModuleNameFrom(String moduleInfoText, String fileName = 'module-info.java') {
        def text = moduleInfoText.replaceAll(COMMENT, '')
        def matcher = MODULE_DECL.matcher(text)
        if(!matcher.matches()) throw new GradleException("Cannot retrieve module name from $fileName with content: $moduleInfoText")
        matcher[0][1]
    }

    static String getDefaultMergedModuleName(Project project) {
        String name = (project.group ?: project.name) as String
        "${Util.toModuleName(name)}.merged.module"
    }

    @CompileDynamic
    static String getDefaultModuleName(Project project) {
        Set<File> srcDirs = project.sourceSets.main?.java?.srcDirs
        File moduleInfoDir = srcDirs?.find { it.list()?.contains('module-info.java')}
        if(!moduleInfoDir) throw new GradleException("Cannot find module-info.java in $srcDirs")
        def moduleInfoFile = new File(moduleInfoDir, 'module-info.java')
        Util.getModuleNameFrom(moduleInfoFile.text, moduleInfoFile.path)
    }

    static String getPackage(String entryName) {
        if(!entryName.endsWith('.class')) return null
        int pos = entryName.lastIndexOf('/')
        if(pos <= 0) return null
        int dotPos = entryName.lastIndexOf('.')
        if(!Utilities.isJavaIdentifier(entryName.substring(pos + 1, dotPos))) return null
        def pkgName = entryName.substring(0, pos).replace('/', '.')
        boolean valid = pkgName.split('\\.').every {String s -> Utilities.isJavaIdentifier(s)}
        return valid ? pkgName : null
    }

    @CompileDynamic
    static String getModuleName(File f) {
        try {
            return ModuleFinder.of(f.toPath()).findAll().first().descriptor().name()
        } catch (Exception e) {
            def modName = getFallbackModuleName(f)
            LOGGER.warn("Cannot retrieve the module name of $f. Using fallback value: $modName.")
            return modName
        }
    }

    static String getFallbackModuleName(File f) {
        def modName = new JarFile(f).getManifest()?.mainAttributes?.getValue('Automatic-Module-Name')
        if(modName) return modName
        def s = f.name
        def tokens = s.split('[_.0-9]*-[0-9]')
        if(tokens.length < 2) return s - '.jar'
        def len = s.length()
        return tokens[0].replace('-', '.').replace('default', 'dflt')
    }

    @CompileDynamic
    static void createManifest(Object targetDir, boolean multiRelease) {
        def mfdir = new File(targetDir, 'META-INF')
        mfdir.mkdirs()
        def mf = new File(mfdir, 'MANIFEST.MF')
        mf.delete()
        mf << """Manifest-Version: 1.0
            Created-By: Badass-JLink Plugin
            Built-By: gradle
        """.stripIndent()
        if(multiRelease) mf << 'Multi-Release: true\n'
    }

    @CompileDynamic
    static void createJar(Project project, String javaHome, String jarFilePath, Object contentDir) {
        project.file(jarFilePath).parentFile.mkdirs()
        project.exec {
            commandLine "$javaHome/bin/jar",
                    '--create',
                    '--file',
                    jarFilePath,
                    '--no-manifest',
                    '-C',
                    contentDir,
                    '.'
        }
    }

    static boolean isValidClassFileReference(String name) {
        if(!name.endsWith('.class')) return false
        name = name - '.class'
        def tokens = name.split('[./\\\\]')
        name = tokens[tokens.length - 1]
        return Utilities.isJavaIdentifier(name)
    }


    static void scan(File file,
         @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        if(!file.exists()) throw new IllegalArgumentException("File or directory not found: $file")
        if(file.directory) scanDir(file, action)
        else scanJar(file, action)
    }

    private static void scanDir(File dir,
                        @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        if(!dir.directory) throw new IllegalArgumentException("Not a directory: $dir")
        dir.eachFileRecurse(FileType.FILES) { file ->
            def basePath = dir.absolutePath.replace('\\', '/')
            def relPath = dir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            IOGroovyMethods.withCloseable(file.newInputStream()) {
                action.call(basePath, relPath, it)
            }
        }
    }

    private static void scanJar(File jarFile,
                        @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        def zipFile = new ZipFile(jarFile)
        zipFile.entries().each { ZipEntry entry ->
            IOGroovyMethods.withCloseable(zipFile.getInputStream(entry)) {
                action.call('', entry.name, it)
            }
        }
    }

    static File getVersionedDir(File baseDir, int javaVersion) {
        def versionsDir = new File("$baseDir.absolutePath/META-INF/versions")
        if(!versionsDir.directory) return null
        def version = versionsDir.listFiles({ File f -> f.directory && f.name.number }as FileFilter)
                .collect {File f -> f.name as int}.findAll{int v -> v <= javaVersion}.max()
        if(!version) return null
        new File(versionsDir, "$version")
    }

    static Set<File> getArtifacts(Set<ResolvedDependency> deps) {
        (Set<File>)deps.collect{ it.moduleArtifacts*.file }.flatten() as Set
    }

    static boolean isEmptyJar(File jarFile) {
            def zipFile = new ZipFile(jarFile)
        zipFile.entries().every { ZipEntry entry -> entry.name in ['META-INF/', 'META-INF/MANIFEST.MF']}
     }

    @CompileDynamic
    static DirectoryProperty createDirectoryProperty(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            return project.layout.directoryProperty()
        } else {
            return project.objects.directoryProperty()
        }
    }

    @CompileDynamic
    static RegularFileProperty createRegularFileProperty(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            return project.layout.fileProperty()
        } else {
            return project.objects.fileProperty()
        }
    }

    static <T> void addToListProperty(ListProperty<T> listProp, T... values) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            def list = new ArrayList(listProp.get())
            list.addAll(values as List)
            listProp.set(list)
        } else {
            listProp.addAll(values as List)
        }
    }

    @CompileDynamic
    static <K,V> Provider<Map<K,V>> createMapProperty(Project project,
                                                      Class<K> keyType, Class<V> valueType) {
        Provider<Map<K,V>> provider
        if(GradleVersion.current() < GradleVersion.version('5.1')) {
            provider = (Property<Map<K,V>>)project.objects.property(Map)
        } else {
            provider = project.objects.mapProperty(keyType, valueType)
        }
        provider.set(new TreeMap<K,V>())
        provider
    }

    @CompileDynamic
    static <K,V> void putToMapProvider(Provider<Map<K,V>> mapProvider, K key, V value) {
        def map = new TreeMap(mapProvider.get())
        map[key] = value
        mapProvider.set(map)
    }
}

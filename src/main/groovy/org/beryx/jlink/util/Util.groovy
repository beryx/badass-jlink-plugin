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
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

import java.lang.module.ModuleDescriptor
import java.lang.module.ModuleFinder
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class Util {
    private static final Logger LOGGER = Logging.getLogger(Util.class)

    static String EXEC_EXTENSION = System.getProperty('os.name', '').toLowerCase().contains('win') ? '.exe' : ''

    private static final String WS = '[ \t\r\n\\u000C]'
    private static final String LINE_COMMENT = '//[^\r\n]*'
    private static final String MULTILINE_COMMENT = '/\\*.*?\\*/'
    private static final String IGNORE =  '(' + WS + '|' + LINE_COMMENT + '|' + MULTILINE_COMMENT + ')*'
    private static final String INNER_IGNORE =  '(' + MULTILINE_COMMENT + ')*'

    private static final String LETTER = '[a-zA-Z$_]|[^\\u0000-\\u007F\\uD800-\\uDBFF]|[\\uD800-\\uDBFF]|[\\uDC00-\\uDFFF]'
    private static final String LETTER_OR_DIGIT = LETTER + '|[0-9]'

    private static final String IDENTIFIER = '((' + LETTER + INNER_IGNORE + ')(' + LETTER_OR_DIGIT + INNER_IGNORE + ')*)'
    private static final String QUALIFIED_NAME = IDENTIFIER + INNER_IGNORE + '(\\.' + INNER_IGNORE + IDENTIFIER + INNER_IGNORE + ')*'

    private static final String IMPORT_DECLARATION = 'import' + IGNORE + '(static' + IGNORE + ')?' + QUALIFIED_NAME + '(\\.\\*' + ')?' + IGNORE + ';'
    private static final String IMPORT_DECLARATIONS = '(' + IMPORT_DECLARATION + IGNORE + ')*'
    private static final String MODULE_DECLARATION = '(?s)' + IGNORE + IMPORT_DECLARATIONS + '(open' + IGNORE + ')?' + 'module' + IGNORE + '(?<MODULE>' + QUALIFIED_NAME + ').*?'

    private static final Pattern PATTERN = Pattern.compile(MODULE_DECLARATION)


    @CompileDynamic
    static String getModuleNameFrom(String moduleInfoText, String fileName = 'module-info.java') {
        def matcher = PATTERN.matcher(moduleInfoText)
        if(!matcher.matches()) throw new GradleException("Cannot retrieve module name from $fileName with content: $moduleInfoText")
        matcher.group("MODULE")
    }


    static String getDefaultMergedModuleName(Project project) {
        String name = (project.group ?: project.name) as String
        "${toModuleName(name)}.merged.module"
    }

    @CompileDynamic
    static String getDefaultModuleName(Project project) {
        Set<File> srcDirs = project.sourceSets.main?.java?.srcDirs
        File moduleInfoDir = srcDirs?.find { it.list()?.contains('module-info.java')}
        if(!moduleInfoDir) throw new GradleException("Cannot find module-info.java in $srcDirs")
        try {
            def moduleInfoFile = new File(moduleInfoDir, 'module-info.java')
            getModuleNameFrom(moduleInfoFile.text, moduleInfoFile.path)
        } catch (GradleException ge) {
            throw ge
        } catch (Exception e) {
            throw new GradleException("Cannot retrieve module name from $moduleInfoDir/module-info.java", e)
        }
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
            def modName = getFallbackModuleNameFromJarFile(f)
            LOGGER.warn("Cannot retrieve the module name of $f. Using fallback value: $modName.")
            return modName
        }
    }

    static String getFallbackModuleNameFromJarFile(File f) {
        def modName = new JarFile(f).getManifest()?.mainAttributes?.getValue('Automatic-Module-Name')
        if(modName) return modName
        return getFallbackModuleNameFromJarName(f.name)
    }

    static String getFallbackModuleNameFromJarName(String s) {
        def tokens = s.split('[_.0-9]*-[0-9]')
        def modName = (tokens.length < 2) ? (s - '.jar') : tokens[0]
        toModuleName(modName)
    }

    static final Set<String> KEYWORDS = [
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto",
            "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "try",
            "void", "volatile", "while"] as HashSet

    static String toModuleName(String s) {
        def name = s.replaceAll('[^0-9A-Za-z_.]', '.')
        int start = 0
        while(name[start] == '.') start++
        int end = name.length() - 1
        while(name[end] == '.') end--
        name = name.substring(start, end + 1)
        name.replaceAll('\\.[.]+', '.')

        def items = name.split('\\.')
            .findAll { it }
            .collect { String it -> KEYWORDS.contains(it) ? "${it}_" : (it[0] as char).isDigit() ? "_$it" : it }
        items.join('.')

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

    static File getArtifact(ResolvedDependency dep) {
        def artifact = dep.moduleArtifacts.find {it.classifier} ?: dep.moduleArtifacts[0]
        artifact.file
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

    @CompileDynamic
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

    @CompileDynamic
    static String getArchiveBaseName(Project project) {
        String name = ""
        try {
            if(GradleVersion.current() < GradleVersion.version('5.1')) {
                name = project.jar.baseName
            } else {
                name = project.jar.archiveBaseName.get()
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot get archiveBaseName: $e")
        }
        name
    }

    static void checkExecutable(String filePath) {
        checkExecutable(new File(filePath))
    }

    static void checkExecutable(File f) {
        if(!f.file) throw new GradleException("$f.absolutePath does not exist.")
        if(!f.canExecute()) throw new GradleException("$f.absolutePath is not executable.")
    }

    static List<File> getJarsAndMods(Object... modulePath) {
        List<File> allFiles = []
        modulePath.each {entry ->
            File f = (entry instanceof File) ? (File)entry : new File(entry.toString())
            if(f.file) allFiles.add(f)
            if(f.directory) {
                allFiles.addAll(f.listFiles({File ff -> ff.file} as FileFilter))
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
            ZipEntry zipEntry = (ZipEntry)entry
            if(zipEntry.name == "${prefix}module-info.class" as String) {
                def entryStream = zipFile.getInputStream(zipEntry)
                return ModuleDescriptor.read(entryStream)
            }
        }
        null
    }

    static List<Project> getAllDependentProjects(Project project) {
        getAllDependentProjectsExt(project, new HashSet<Project>())
    }

    private static List<Project> getAllDependentProjectsExt(Project project, Set<Project> handledProjects) {
        if(handledProjects.contains(project)) return []
        handledProjects << project
        def projectDependencies = project.configurations*.dependencies*.withType(ProjectDependency).flatten() as Set<ProjectDependency>
        List<Project> dependentProjects = projectDependencies*.dependencyProject
        dependentProjects.each {
            dependentProjects += getAllDependentProjectsExt(it, handledProjects)
        }
        return dependentProjects.unique()
    }
}

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
import org.beryx.jlink.data.PrepareMergedJarsDirTaskData
import org.beryx.jlink.util.DependencyManager
import org.beryx.jlink.util.Util
import org.codehaus.groovy.tools.Utilities
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class PrepareMergedJarsDirTaskImpl extends BaseTaskImpl<PrepareMergedJarsDirTaskData> {
    private static final Logger LOGGER = Logging.getLogger(PrepareMergedJarsDirTaskImpl.class);
    private static final int MAX_LISTED_CONFLICTING_KEYS = 5

    final FileSystemOperations fileSystemOperations
    final ArchiveOperations archiveOperations

    PrepareMergedJarsDirTaskImpl(FileSystemOperations fileSystemOperations, ArchiveOperations archiveOperations, PrepareMergedJarsDirTaskData taskData) {
        super(taskData)
        this.fileSystemOperations = fileSystemOperations
        this.archiveOperations = archiveOperations
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        fileSystemOperations.delete { it.delete(td.jlinkBasePath) }
        td.mergedJarsDir.mkdirs()
        copyRuntimeJars()
        // sorted, so that the merge order does not depend on the file system
        mergeUnpackedContents((new File(td.nonModularJarsDirPath).listFiles() as List).sort { it.name })
    }

    @CompileDynamic
    def copyRuntimeJars() {
        fileSystemOperations.delete { it.delete(td.jlinkJarsDirPath, td.nonModularJarsDirPath) }
        new File(td.jlinkJarsDirPath).mkdirs()
        new File(td.nonModularJarsDirPath).mkdirs()
        LOGGER.info("Copying modular jars required by non-modular jars to ${td.jlinkJarsDirPath}...")
        td.modularJarsRequiredByNonModularJars.each { jar ->
            LOGGER.debug("\t... from $jar ...")
            fileSystemOperations.copy {
                into td.jlinkJarsDirPath
                from jar
            }
        }
        LOGGER.info("Copying mon-modular jars to ${td.nonModularJarsDirPath}...")
        td.nonModularJars.each { jar ->
            fileSystemOperations.copy {
                into td.nonModularJarsDirPath
                from jar
            }
        }
    }

    @CompileDynamic
    def mergeUnpackedContents(Collection<File> jars) {
        if(jars.empty) return
        LOGGER.info("Merging content into ${td.mergedJarsDir}...")

        TreeMap<String, String> services = [:]
        jars.each { jar ->
            LOGGER.debug("Merging ${jar}...")
            try {
                fileSystemOperations.delete { it.delete(td.tmpJarsDirPath) }
                List<String> excludesOfJar = getExcludesOf(jar)
                fileSystemOperations.copy {
                    from archiveOperations.zipTree(jar)
                    into td.tmpJarsDirPath
                    exclude 'module-info.class', 'META-INF/services/*', 'META-INF/INDEX.LIST', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'META-INF/SIG-*'
                    exclude { hasInvalidName(it) }
                    if(excludesOfJar) {
                        exclude excludesOfJar
                    }
                }

                appendServices(services, jar)

                def versionedDir = Util.getVersionedDir(new File(td.tmpJarsDirPath), td.jvmVersion)
                if(versionedDir?.directory) {
                    fileSystemOperations.copy {
                        from versionedDir
                        into td.tmpJarsDirPath
                        exclude 'module-info.class'
                    }
                }
                if(new File(td.tmpJarsDirPath).directory) {
                    fileSystemOperations.delete { it.delete("$td.tmpJarsDirPath/META-INF/versions") }
                    fileSystemOperations.copy {
                        from td.tmpJarsDirPath
                        into td.mergedJarsDir
                        eachFile { FileCopyDetails details ->
                            if(details.path.endsWith('.properties')) {
                                def target = new File(td.mergedJarsDir, details.path)
                                if(target.file) {
                                    mergePropertiesFile(details.path, target, details.file, jar)
                                    details.exclude()
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to merge unpacked content of $jar")
                throw e
            }
        }
        writeServiceFiles(services)
        Util.createManifest(td.mergedJarsDir, false)
    }

    /**
     * Appends the content of a properties file to a properties file with the same path, previously
     * copied from another jar, so that the merged module contains the entries of both.
     * On duplicate keys, {@link Properties} uses the last value, that is, the one from {@code jar}.
     */
    private static void mergePropertiesFile(String path, File target, File source, File jar) {
        // ISO-8859-1 maps bytes one-to-one to chars, so files in other encodings are preserved as is
        String targetText = target.getText('ISO-8859-1')
        String sourceText = source.getText('ISO-8859-1')
        if(targetText == sourceText) return
        Properties targetProperties = loadProperties(targetText)
        Properties sourceProperties = loadProperties(sourceText)
        List<String> conflictingKeys = sourceProperties.stringPropertyNames().findAll { String key ->
            targetProperties.containsKey(key) && targetProperties.getProperty(key) != sourceProperties.getProperty(key)
        }.sort()
        if(conflictingKeys) {
            String listedKeys = conflictingKeys.take(MAX_LISTED_CONFLICTING_KEYS).join(', ')
            if(conflictingKeys.size() > MAX_LISTED_CONFLICTING_KEYS) {
                listedKeys += " and ${conflictingKeys.size() - MAX_LISTED_CONFLICTING_KEYS} more"
            }
            LOGGER.warn("Merging $path from ${jar.name} into the merged module: conflicting values for [$listedKeys], using the values from ${jar.name}.")
        } else {
            LOGGER.info("Merging $path from ${jar.name} into the merged module.")
        }
        if(targetText && !targetText.endsWith('\n') && !targetText.endsWith('\r')) {
            target.append('\n', 'ISO-8859-1')
        }
        target.append(sourceText, 'ISO-8859-1')
    }

    private static Properties loadProperties(String text) {
        def properties = new Properties()
        properties.load(new StringReader(text))
        properties
    }

    private List<String> getExcludesOf(File jar) {
        List<String> excludes = []
        td.jarExcludes.each {prefix, list ->
            if(jar.name.startsWith(prefix)) {
                excludes.addAll(list)
            }
        }
        excludes
    }

    @CompileDynamic
    private static boolean hasInvalidName(FileTreeElement fte) {
        String path = fte.path
        if(fte.directory) return false
        if(path.startsWith('META-INF')) return false
        if(!path.endsWith('.class')) return false
        String[] tokens = path.split('/')
        if(tokens.length > 0) {
            tokens = ((tokens.length == 1) ? [] : tokens[0 .. -2]) as String[]
        }
        def invalid = !tokens.every { String token -> Utilities.isJavaIdentifier(token) }
        if(invalid) {
            LOGGER.warn("Excluding $path from the merged module.")
        }
        return invalid
    }

    @CompileDynamic
    void appendServices(Map<String, String> services, File jar) {
        def svcFiles = archiveOperations.zipTree(jar).matching {
            include 'META-INF/services/*'
        }
        svcFiles?.files?.each { f ->
            def oldText = services[f.name] ?: ''
            if(oldText && !oldText.endsWith('\n')) oldText += '\n'
            services[f.name] = oldText + f.text
        }
    }

    void writeServiceFiles(Map<String, String> services) {
        def svcDir = new File("$td.mergedJarsDir/META-INF/services")
        svcDir.mkdirs()
        services.each { name, text ->
            new File(svcDir, name).write(text)
        }
    }
}

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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.beryx.jlink.util.Util.getArtifacts
import static org.beryx.jlink.util.Util.isEmptyJar

@CompileStatic
class DependencyManager {
    private static final Logger LOGGER = Logging.getLogger(DependencyManager.class)

    final Project project
    final List<String> forceMergedJarPrefixes
    final List<String> extraDependenciesPrefixes

    final Set<DependencyExt> allDependencies

    private final Set<DependencyExt> extraDeps
    private final Set<DependencyExt> modularDeps
    private final Set<DependencyExt> nonModularDeps
    private final Set<DependencyExt> modularDepsRequiredByNonModularJars

    private final Set<File> artifactsHandledAsNonModular

    @Canonical
    static class DependencyExt {
        ResolvedDependency dependency
        File artifact

        @Override
        String toString() {
            "$artifact.name / $dependency"
        }
    }

    DependencyManager(Project project, List<String> forceMergedJarPrefixes, List<String> extraDependenciesPrefixes, Configuration configuration) {
        this.project = project
        this.forceMergedJarPrefixes = forceMergedJarPrefixes
        this.extraDependenciesPrefixes = extraDependenciesPrefixes
        allDependencies = []
        configuration.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
            getArtifacts([dep] as Set).each { f ->
                def depExt = new DependencyExt(dependency: dep, artifact: f)
                if(!isEmptyJar(f)) {
                    allDependencies << depExt
                }
                collectAllDescendants(depExt, allDependencies)
            }
        }
        Set<File> depFiles = allDependencies*.artifact as Set
        configuration.resolvedConfiguration.files.each { f ->
            if(!isEmptyJar(f) && !depFiles.contains(f)) {
                allDependencies << new DependencyExt(artifact: f)
            }
        }

        this.extraDeps = allDependencies.findAll{ DependencyExt dep ->
            extraDependenciesPrefixes.any { dep.artifact.name.startsWith(it) }
        }
        LOGGER.info("extraDeps: $extraDeps")

        artifactsHandledAsNonModular = []
        while(true) {
            def dep = getModularThatShouldBeHandledAsNonModular()
            if(!dep) break
            artifactsHandledAsNonModular << dep.artifact
            LOGGER.info("Handling $dep.artifact.name as non-modular")
        }

        modularDeps = allDependencies.findAll{ !isHandledAsNonModular(it.artifact) }
        nonModularDeps = allDependencies.findAll{ isHandledAsNonModular(it.artifact) }

        Set<DependencyExt> handledDeps = []
        modularDepsRequiredByNonModularJars = extraDeps.findAll{!isHandledAsNonModular(it.artifact)}

        nonModularDeps.each { collectModularJarsRequiredByNonModularJars(it, modularDepsRequiredByNonModularJars, modularDeps, handledDeps) }

        LOGGER.info("modularJars: ${modularJars*.name}")
        LOGGER.info("nonModularJars: ${nonModularJars*.name}")
        LOGGER.info("modularJarsRequiredByNonModularJars: ${modularJarsRequiredByNonModularJars*.name}")
        LOGGER.info("artifactsHandledAsNonModular: ${artifactsHandledAsNonModular*.name}")
    }

    Set<File> getModularJarsRequiredByNonModularJars() {
        modularDepsRequiredByNonModularJars*.artifact as Set
    }

    Set<File> getModularJars() {
        modularDeps*.artifact as Set
    }

    Set<File> getNonModularJars() {
        nonModularDeps*.artifact as Set
    }

    private DependencyExt getModularThatShouldBeHandledAsNonModular() {
        Set<DependencyExt> handledDeps = []
        Set<DependencyExt> modsRequiredByNonMods = extraDeps.findAll {!isHandledAsNonModular(it.artifact)}
        Set<DependencyExt> nonModDeps = allDependencies.findAll { isHandledAsNonModular(it.artifact) }
        Set<DependencyExt> modDeps = allDependencies.findAll { !isHandledAsNonModular(it.artifact) }
        LOGGER.debug("nonModDeps: ${nonModDeps*.artifact.name}")
        nonModDeps.each {collectModularJarsRequiredByNonModularJars(it, modsRequiredByNonMods, modDeps, handledDeps)}
        LOGGER.debug("modsRequiredByNonMods: ${modsRequiredByNonMods*.artifact.name}")
        for(DependencyExt dep: modsRequiredByNonMods) {
            if(getDescendants(dep) {isHandledAsNonModular(it)}) return dep
        }
        null
    }

    private Set<DependencyExt> getDescendants(DependencyExt dep, Predicate<File> filter) {
        Set<DependencyExt> descendants = []
        collectDescendants(dep, descendants, filter)
        descendants
    }

    private void collectDescendants(DependencyExt dep, Set<DependencyExt> descendants, Predicate<File> filter) {
        if(dep.dependency) {
            for(ResolvedDependency child: dep.dependency.children) {
                getArtifacts([child] as Set).each { f ->
                    def childExt = new DependencyExt(dependency: child, artifact: f)
                    if(!isEmptyJar(f) && filter.test(f)) {
                        descendants << childExt
                    }
                    collectDescendants(childExt, descendants, filter)
                }
            }
        }
    }

    private void collectAllDescendants(DependencyExt dep, Set<DependencyExt> descendants) {
        if(dep.dependency) {
            for(ResolvedDependency child: dep.dependency.children) {
                getArtifacts([child] as Set).each { f ->
                    def childExt = new DependencyExt(dependency: child, artifact: f)
                    if(!isEmptyJar(f)) {
                        descendants << childExt
                        if(!(childExt in descendants)) {
                            collectAllDescendants(childExt, descendants)
                        }
                    }
                }
            }
        }
    }

    private static final Pattern MULTI_RELEASE_MODULE_INFO = ~'META-INF/versions/[0-9]+/module-info.class'
    private boolean isHandledAsNonModular(File artifact) {
        if(!artifact) return true
        if(isEmptyJar(artifact)) return true
        if(artifactsHandledAsNonModular.contains(artifact)) return true
        if(forceMergedJarPrefixes.any {artifact.name.startsWith(it)}) return true
        new ZipFile(artifact).entries().every { ZipEntry entry ->
            (entry.name != 'module-info.class') && (!entry.name.matches(MULTI_RELEASE_MODULE_INFO))
        }
    }

    private void collectModularJarsRequiredByNonModularJars(DependencyExt dep, Set<DependencyExt> collectedDeps,
                                                        Set<DependencyExt> modDeps, Set<DependencyExt> handledDeps) {
        if(!handledDeps.add(dep)) return
        if(modDeps.contains(dep)) collectedDeps.add(dep)
        if(dep.dependency) {
            dep.dependency.children.each { childDep ->
                getArtifacts([childDep] as Set).each { f ->
                    def childExt = new DependencyExt(dependency: childDep, artifact: f)
                    collectModularJarsRequiredByNonModularJars(childExt, collectedDeps, modDeps, handledDeps)
                }
            }
        }
    }
}

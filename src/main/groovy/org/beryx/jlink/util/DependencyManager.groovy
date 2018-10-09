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
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

import java.util.function.Predicate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class DependencyManager {
    final Project project
    final List<String> forceMergedJarPrefixes

    final Set<File> modularJars = []
    final Set<File> nonModularJars = []
    final Set<File> modularJarsRequiredByNonModularJars = []

    private final Set<ResolvedDependency> dependenciesHandledAsNonModular = []

    DependencyManager(Project project, List<String> forceMergedJarPrefixes) {
        this.project = project
        this.forceMergedJarPrefixes = forceMergedJarPrefixes
        while(true) {
            def dep = getModularThatShouldBeHandledAsNonModular()
            if(!dep) break
            dependenciesHandledAsNonModular << dep
            project.logger.info("Handling $dep.name as non-modular")
        }

        Set<ResolvedDependency> modDeps = collectDeps {!isHandledAsNonModular(it)}
        modularJars = modDeps.collect{ dep -> dep.moduleArtifacts[0].file} as Set

        Set<ResolvedDependency> nonModDeps = collectDeps {isHandledAsNonModular(it)}
        nonModularJars = nonModDeps.collect{ dep -> dep.moduleArtifacts[0].file} as Set

        Set<ResolvedDependency> handledDeps = []
        Set<ResolvedDependency> modsRequiredByNonMods = []

        nonModDeps.each {collectModularJarsRequiredByNonModularJars(it, modsRequiredByNonMods, modDeps, handledDeps)}
        modularJarsRequiredByNonModularJars = modsRequiredByNonMods.collect{ dep -> dep.moduleArtifacts[0].file} as Set

        project.logger.info("modularJars: ${modularJars*.name}")
        project.logger.info("nonModularJars: ${nonModularJars*.name}")
        project.logger.info("modularJarsRequiredByNonModularJars: ${modularJarsRequiredByNonModularJars*.name}")
        project.logger.info("dependenciesHandledAsNonModular: ${dependenciesHandledAsNonModular*.name}")
    }

    private Set<ResolvedDependency> collectDeps(Predicate<ResolvedDependency> filter) {
        Set<ResolvedDependency> deps = []
        for(ResolvedDependency dep: project.configurations['runtimeClasspath'].resolvedConfiguration.firstLevelModuleDependencies) {
            if(filter.test(dep)) deps << dep
            deps.addAll(getDescendants(dep, filter))
        }
        deps
    }

    private ResolvedDependency getModularThatShouldBeHandledAsNonModular() {
        Set<ResolvedDependency> handledDeps = []
        Set<ResolvedDependency> modsRequiredByNonMods = []
        Set<ResolvedDependency> nonModDeps = collectDeps{ isHandledAsNonModular(it) }
        Set<ResolvedDependency> modDeps = collectDeps {!isHandledAsNonModular(it)}
        project.logger.debug("nonModDeps: ${nonModDeps*.name}")
        nonModDeps.each {collectModularJarsRequiredByNonModularJars(it, modsRequiredByNonMods, modDeps, handledDeps)}
        project.logger.debug("modsRequiredByNonMods: ${modsRequiredByNonMods*.name}")
        for(ResolvedDependency dep: modsRequiredByNonMods) {
            if(getDescendants(dep) {isHandledAsNonModular(it)}) return dep
        }
        null
    }

    private Set<ResolvedDependency> getDescendants(ResolvedDependency dep, Predicate<ResolvedDependency> filter) {
        Set<ResolvedDependency> descendants = []
        collectDescendants(dep, descendants, filter)
        descendants
    }

    private void collectDescendants(ResolvedDependency dep, Set<ResolvedDependency> descendants, Predicate<ResolvedDependency> filter) {
        for(ResolvedDependency child: dep.children) {
            if(filter.test(child)) {
                descendants << child
            }
            collectDescendants(child, descendants, filter)
        }
    }

    private boolean isHandledAsNonModular(ResolvedDependency dep) {
        if(dependenciesHandledAsNonModular.contains(dep)) return true
        def f = dep.moduleArtifacts[0].file
        if(forceMergedJarPrefixes.any {f.name.startsWith(it)}) return true
        new ZipFile(f).entries().every { ZipEntry entry ->
            (entry.name != 'module-info.class') && (!entry.name.matches('META-INF/versions/[0-9]+/module-info.class'))
        }
    }

    private void collectModularJarsRequiredByNonModularJars(ResolvedDependency dep, Set<ResolvedDependency> collectedJars,
                                                        Set<ResolvedDependency> modDeps, Set<ResolvedDependency> handledDeps) {
        if(!handledDeps.add(dep)) return
        if(modDeps.contains(dep)) collectedJars.add(dep)
        dep.children.each {collectModularJarsRequiredByNonModularJars(it, collectedJars, modDeps, handledDeps)}
    }
}

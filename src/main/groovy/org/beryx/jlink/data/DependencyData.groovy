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
package org.beryx.jlink.data

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static org.beryx.jlink.util.Util.getArtifacts
import static org.beryx.jlink.util.Util.isEmptyJar

/**
 * Serializable data structure that captures dependency information at configuration time
 * for use during task execution, including with configuration cache.
 */
@CompileStatic
class DependencyData implements Serializable {
    private static final Logger LOGGER = Logging.getLogger(DependencyData.class)
    private static final long serialVersionUID = 1L

    /**
     * Map from artifact file to its parent artifact files (dependencies)
     */
    Map<File, Set<File>> dependencyTree = [:]

    /**
     * All artifact files
     */
    Set<File> allArtifacts = []

    /**
     * Creates DependencyData by extracting information from a Configuration
     */
    static DependencyData from(Configuration configuration) {
        def data = new DependencyData()

        // Build dependency tree
        configuration.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
            data.collectDependencies(dep, null)
        }

        // Add all files from configuration (including those without dependency metadata)
        configuration.incoming.files.each { f ->
            if (!isEmptyJar(f)) {
                data.allArtifacts << f
            }
        }

        LOGGER.info("Extracted dependency data: ${data.allArtifacts.size()} artifacts, ${data.dependencyTree.size()} with dependencies")
        return data
    }

    private void collectDependencies(ResolvedDependency dep, File parent) {
        getArtifacts([dep] as Set).each { File artifact ->
            if (!isEmptyJar(artifact)) {
                allArtifacts << artifact

                if (parent != null) {
                    dependencyTree.computeIfAbsent(artifact, { [] as Set }).add(parent)
                }

                // Recursively collect children
                dep.children.each { child ->
                    collectDependencies(child, artifact)
                }
            }
        }
    }

    /**
     * Gets all parent artifacts (dependencies) of the given artifact
     */
    Set<File> getParents(File artifact) {
        dependencyTree.get(artifact) ?: ([] as Set)
    }

    /**
     * Gets all child artifacts (dependents) of the given artifact
     */
    Set<File> getChildren(File artifact) {
        def children = [] as Set<File>
        dependencyTree.each { child, parents ->
            if (parents.contains(artifact)) {
                children << child
            }
        }
        return children
    }
}

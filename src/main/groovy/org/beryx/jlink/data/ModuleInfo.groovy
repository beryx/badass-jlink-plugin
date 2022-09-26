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

import groovy.transform.EqualsAndHashCode
import groovy.transform.MapConstructor
import groovy.transform.ToString

import java.util.stream.Collectors

@ToString(includeNames = true)
class ModuleInfo implements Serializable, GroovyObject {
    String version
    boolean enabled
    boolean additive
    final AdditiveConstraints additiveConstraints = new AdditiveConstraints()
    final List<RequiresBuilder> requiresBuilders = []
    final List<UsesBuilder> usesBuilders = []
    final List<ProvidesBuilder> providesBuilders = []

    private boolean afterRequiresTransitive = false

    static enum Language {JAVA, GROOVY, KOTLIN}

    static class AdditiveConstraints implements Serializable {
        boolean enabled = false
        final Set<String> excludedRequires = []
        final Set<String> excludedUses = []
        final List<ProvidesConstraint> excludedProvidesConstraints = []
    }

    void excludeRequires(String... modules) {
        additiveConstraints.enabled = true
        additiveConstraints.excludedRequires.addAll(modules)
    }

    void excludeUses(String... services) {
        additiveConstraints.enabled = true
        additiveConstraints.excludedUses.addAll(services)
    }

    void excludeProvides(Map constraints) {
        additiveConstraints.enabled = true
        additiveConstraints.excludedProvidesConstraints.add(new ProvidesConstraint(constraints))
    }

    @MapConstructor
    @ToString
    static class ProvidesConstraint implements Serializable{
        String service
        String implementation
        String servicePattern
        String implementationPattern

        boolean matches(String svc, String impl) {
            if(service && svc != service) return false
            if(implementation && impl != implementation) return false
            if(servicePattern && !(svc ==~ servicePattern)) return false
            if(implementationPattern && !(impl ==~ implementationPattern)) return false
            true
        }
    }

    boolean shouldUseSuggestions() {
        !enabled || additive || additiveConstraints.enabled
    }

    @EqualsAndHashCode
    static class RequiresBuilder implements Serializable {
        final String module
        boolean transitive = false

        RequiresBuilder(String module) {
            this.module = module
        }

        RequiresBuilder withTransitive(boolean transitive) {
            this.transitive = transitive
            this
        }

        @Override
        String toString() {
            toString(Language.JAVA)
        }
        String toString(Language language) {
            def q = (language == Language.JAVA) ? "" : "'"
            if(language == Language.KOTLIN) {
                return "requires${transitive ? 'Transitive' : ''}(\"$module\");"
            } else {
                return "requires ${transitive ? 'transitive ' : ''}$q$module$q;"
            }
        }

        String get() {
            if(!module) throw new IllegalStateException("No module provided")
            toString()
        }
    }

    RequiresBuilder requires(String module) {
        afterRequiresTransitive = false
        def builder = new RequiresBuilder(module)
        requiresBuilders << builder
        builder
    }

    RequiresBuilder requiresTransitive(String module) {
        afterRequiresTransitive = false
        def builder = new RequiresBuilder(module).withTransitive(true)
        requiresBuilders << builder
        builder
    }

    Transitive requires(Transitive transitive) {
        afterRequiresTransitive = true
        transitive
    }

    class Transitive implements Serializable {}
    final transitive = new Transitive()
    def propertyMissing(String name) {
        if(afterRequiresTransitive) {
            afterRequiresTransitive = false
            def builder = new RequiresBuilder(name).withTransitive(true)
            requiresBuilders << builder
            return builder
        }
        throw new MissingPropertyException(name, this.getClass())
    }


    @EqualsAndHashCode
    static class UsesBuilder implements Serializable {
        final String service

        UsesBuilder(String service) {
            this.service = adjustQualifiedName(service)
        }

        @Override
        String toString() {
            toString(Language.JAVA)
        }
        String toString(Language language) {
            def q = (language == Language.JAVA) ? "" : "'"
            if(language == Language.KOTLIN) {
                return "uses(\"$service\");"
            } else {
                return "uses $q$service$q;"
            }
        }

        String get() {
            if(!service) throw new IllegalStateException("No service provided")
            toString()
        }
    }

    UsesBuilder uses(String service) {
        afterRequiresTransitive = false
        def builder = new UsesBuilder(service)
        usesBuilders << builder
        builder
    }

    @EqualsAndHashCode
    static class ProvidesBuilder implements Serializable {
        final String service
        final TreeSet<String> implementations = new TreeSet<>()

        ProvidesBuilder(String service) {
            this.service = adjustQualifiedName(service)
        }

        ProvidesBuilder with(String... implementations) {
            for(s in implementations) {
                this.implementations.add(adjustQualifiedName(s))
            }
            this
        }

        @Override
        String toString() {
            toString(Language.JAVA)
        }
        String toString(Language language) {
            def q = (language == Language.JAVA) ? "" : "'"
            if(language == Language.KOTLIN) {
                def impl = implementations.stream()
                        .map{"\"$it\"" as CharSequence}
                        .collect(Collectors.joining(',\n\t\t\t\t'))
                return "provides(\"$service\").with($impl);"
            } else {
                def impl = implementations.stream()
                        .map{"$q$it$q" as CharSequence}
                        .collect(Collectors.joining(',\n\t\t\t\t'))
                return "provides $q$service$q with $impl;"
            }
        }

        String get() {
            if(!service) throw new IllegalStateException("No service provided")
            if(!implementations) throw new IllegalStateException("No implementations provided for service $service")
            toString()
        }
    }

    ProvidesBuilder provides(String service) {
        afterRequiresTransitive = false
        def builder = new ProvidesBuilder(service)
        providesBuilders << builder
        builder
    }

    @Override
    String toString() {
        return toString(0)
    }

    String toString(int indent, Language language = Language.JAVA) {
        def entries = []
        String blanks = ' ' * indent
        for(builder in requiresBuilders) { entries << blanks + builder.toString(language)}
        for(builder in usesBuilders) { entries << blanks + builder.toString(language)}
        for(builder in providesBuilders) { entries << blanks + builder.toString(language)}
        entries.join('\n')
    }

    private static String adjustQualifiedName(String s) {
        int idx = s.indexOf('.$')
        String notAdjusted = (idx < 0) ? '' : s.substring(idx)
        String toAdjust =  (idx < 0) ? s : s.substring(0, idx)
        String adjusted = toAdjust.replaceAll('([^.])\\$', '$1.')
        return adjusted + notAdjusted
    }
}

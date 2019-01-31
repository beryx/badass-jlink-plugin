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
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.util.stream.Collectors

@CompileStatic
@ToString(includeNames = true)
class ModuleInfo implements Serializable {
    boolean enabled
    boolean additive
    List<RequiresBuilder> requiresBuilders = []
    List<UsesBuilder> usesBuilders = []
    List<ProvidesBuilder> providesBuilders = []

    private boolean afterRequiresTransitive = false

    static enum Language {JAVA, GROOVY, KOTLIN}

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
            this.service = service.replace('$', '.')
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
            this.service = service.replace('$', '.')
        }

        ProvidesBuilder with(String... implementations) {
            for(s in implementations) {
                this.implementations.add(s.replace('$', '.'))
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
}

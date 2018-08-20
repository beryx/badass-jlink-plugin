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
package org.beryx.jlink.taskdata

class ModuleInfo implements Serializable {
    List<RequiresBuilder> requiresBuilders = []
    List<UsesBuilder> usesBuilders = []
    List<ProvidesBuilder> providesBuilders = []

    private boolean afterRequiresTransitive = false

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
            return "requires ${transitive ? 'transitive ' : ''}$module;"
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


    static class UsesBuilder implements Serializable {
        final String module

        UsesBuilder(String module) {
            this.module = module
        }

        @Override
        String toString() {
            return "uses $module;"
        }

        String get() {
            if(!module) throw new IllegalStateException("No module provided")
            toString()
        }
    }

    UsesBuilder uses(String module) {
        afterRequiresTransitive = false
        def builder = new UsesBuilder(module)
        usesBuilders << builder
        builder
    }

    static class ProvidesBuilder implements Serializable {
        final String service
        String implementation

        ProvidesBuilder(String service) {
            this.service = service
        }

        ProvidesBuilder with(String implementation) {
            this.implementation = implementation
            this
        }

        String toString() {
            "provides $service with $implementation;"
        }

        String get() {
            if(!service) throw new IllegalStateException("No service provided")
            if(!implementation) throw new IllegalStateException("No implementation provided for service $service")
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

    String toString(int indent) {
        (requiresBuilders.collect {b -> (' ' * indent) + b} +
        usesBuilders.collect {b -> (' ' * indent) + b} +
        providesBuilders.collect {b -> (' ' * indent) + b}).join('\n')
    }
}

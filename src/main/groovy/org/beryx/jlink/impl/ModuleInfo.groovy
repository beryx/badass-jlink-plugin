package org.beryx.jlink.impl

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

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



// Adapted from: https://github.com/moditect/moditect/blob/master/core/src/main/java/org/moditect/internal/analyzer/ServiceLoaderUseScanner.java
// See original copyright notice below

/**
 *  Copyright 2017 - 2018 The ModiTect authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.beryx.jlink.util

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import groovyjarjarasm.asm.*

import static org.beryx.jlink.data.ModuleInfo.UsesBuilder

@CompileStatic
class ServiceLoaderUseScanner {
    private static final Logger LOGGER = Logging.getLogger(ServiceLoaderUseScanner.class);

    final Set<UsesBuilder> builders = new HashSet<>()

    private String lastClassName
    private String lastMethodName
    private Set<String> unresolvedInvocations = []

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file, { String basePath, String path, InputStream inputStream ->
            if(Util.isValidClassFileReference(path)) {
                LOGGER.trace("scanning ServiceLoader use in: $path")
                try {
                    def cv = new ServiceLoaderClassVisitor()
                    new ClassReader(inputStream).accept(cv, 0)
                    cv.usedServices.each {service -> builders << new UsesBuilder(service)}
                } catch (Exception e) {
                    LOGGER.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        } as Closure)
        invalidEntries
    }

    private class ServiceLoaderClassVisitor extends ClassVisitor {
        final Set<String> usedServices = new HashSet<>()

        ServiceLoaderClassVisitor(){
            super(Opcodes.ASM9)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            lastClassName = name
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            lastMethodName = name
            new ServiceLoaderMethodVisitor(usedServices)
        }
    }

    private class ServiceLoaderMethodVisitor extends MethodVisitor {
        final Set<String> usedServices
        private Type lastType

        ServiceLoaderMethodVisitor(Set<String> usedServices){
            super(Opcodes.ASM9)
            this.usedServices = usedServices
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ((owner == 'java/util/ServiceLoader') && (name == 'load')) {
                LOGGER.info("found ServiceLoader.load() in ${lastClassName}.${lastMethodName}()")
                if (!lastType) {
                    String invocation = "$lastClassName.$lastMethodName"
                    if(!(invocation in unresolvedInvocations)) {
                        unresolvedInvocations.add(invocation)
                        LOGGER.warn( "Cannot derive uses clause from service loader invocation in: $invocation().")
                    }
                } else {
                    usedServices << lastType.className
                }
            }
            lastType = null
        }

        @Override
        void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                lastType = (Type) cst
            }
        }
    }
}

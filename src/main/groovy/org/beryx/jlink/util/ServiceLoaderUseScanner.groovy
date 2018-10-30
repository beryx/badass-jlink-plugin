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

import org.gradle.api.Project
import org.objectweb.asm.*
import static org.beryx.jlink.data.ModuleInfo.UsesBuilder

class ServiceLoaderUseScanner {
    final Project project
    final Set<UsesBuilder> builders = new HashSet<>()


    ServiceLoaderUseScanner(Project project) {
        this.project = project
    }

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file) { basePath, path, inputStream ->
            if(Util.isValidClassFileReference(path)) {
                if(project) project.logger.trace("scanning ServiceLoader use in: $path")
                try {
                    def cv = new ServiceLoaderClassVisitor()
                    new ClassReader(inputStream).accept(cv, 0)
                    cv.usedServices.each {service -> builders << new UsesBuilder(service)}
                } catch (Exception e) {
                    if(project) project.logger.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        }
        invalidEntries
    }

    private class ServiceLoaderClassVisitor extends ClassVisitor {
        final Set<String> usedServices = new HashSet<>()

        ServiceLoaderClassVisitor(){
            super(Opcodes.ASM6)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            new ServiceLoaderMethodVisitor(usedServices)
        }
    }

    private class ServiceLoaderMethodVisitor extends MethodVisitor {
        final Set<String> usedServices
        private Type lastType

        ServiceLoaderMethodVisitor(Set<String> usedServices){
            super(Opcodes.ASM6)
            this.usedServices = usedServices
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ((owner == 'java/util/ServiceLoader') && (name == 'load')) {
                if (!lastType) {
                    if(project) project.logger.warn( "Cannot derive uses clause from service loader invocation with non constant class literal" )
                } else {
                    usedServices << lastType.className
                }
            }
        }

        @Override
        void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                lastType = (Type) cst
            }
        }
    }
}

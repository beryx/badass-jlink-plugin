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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

@CompileStatic
class PackageUseScanner extends ClassVisitor {
    private static final Logger LOGGER = Logging.getLogger(String.class);
    
    final PackageCollection usedPackages = new PackageCollection()
    final PackageCollection ownPackages = new PackageCollection()

    PackageUseScanner() {
        super(Opcodes.ASM7)
    }

    Collection<String> getExternalPackages() {
        usedPackages.packages - ownPackages.packages
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ownPackages.addClass(name)
        if(superName) usedPackages.addClass(superName)
        for(intf in interfaces) {usedPackages.addClass(intf)}
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        usedPackages.addDescriptor(descriptor)
        return super.visitField(access, name, descriptor, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if(exceptions) {
            for(e in exceptions) {
                usedPackages.addClass(e)
            }
        }
        usedPackages.addDescriptor(descriptor)
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file, { String basePath, String path, InputStream inputStream ->
            if(Util.isValidClassFileReference(path)) {
                LOGGER.trace("processing: $path")
                try {
                    ClassReader cr = new ClassReader(inputStream)
                    cr.accept(this, 0)
                } catch (Exception e) {
                    LOGGER.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        } as Closure)
        invalidEntries
    }
}

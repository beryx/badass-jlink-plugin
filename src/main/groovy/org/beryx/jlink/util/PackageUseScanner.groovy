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

import jdk.internal.org.objectweb.asm.Opcodes
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor

class PackageUseScanner extends ClassVisitor {
    final Project project
    final PackageCollection usedPackages = new PackageCollection()
    final PackageCollection ownPackages = new PackageCollection()

    PackageUseScanner(Project project) {
        super(Opcodes.ASM6)
        this.project = project
    }

    Collection<String> getExternalPackages() {
        usedPackages.packages - ownPackages.packages
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ownPackages.addClass(name)
        if(superName) usedPackages.addClass(superName)
        interfaces.each {usedPackages.addClass(it)}
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        usedPackages.addDescriptor(descriptor)
        return super.visitField(access, name, descriptor, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        exceptions.each {usedPackages.addClass(it)}
        usedPackages.addDescriptor(descriptor)
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file) { basePath, path, inputStream ->
            if(Util.isValidClassFileReference(path)) {
                if(project) project.logger.trace("processing: $path")
                try {
                    ClassReader cr = new ClassReader(inputStream)
                    cr.accept(this, 0)
                } catch (Exception e) {
                    if(project) project.logger.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        }
        invalidEntries
    }
}

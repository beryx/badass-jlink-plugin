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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.objectweb.asm.*

@CompileStatic
class PackageUseScanner extends ClassVisitor {
    private static final Logger LOGGER = Logging.getLogger(PackageUseScanner.class);
    
    final PackageCollection usedPackages = new PackageCollection()
    final PackageCollection ownPackages = new PackageCollection()

    private String currentClassName

    private class ScannerMethodVisitor extends MethodVisitor {
        ScannerMethodVisitor(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor)
        }

        @Override
        void visitTypeInsn(int opcode, String type) {
            LOGGER.debug "visitTypeInsn($type)"
            usedPackages.addClass(type)
            super.visitTypeInsn(opcode, type)
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
            visitMethodInsn(opcode, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE)
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            LOGGER.debug "visitMethodInsn($owner)"
            usedPackages.addClass(owner)
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }

        @Override
        void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            LOGGER.debug "visitInvokeDynamic($descriptor)"
            usedPackages.addDescriptor(descriptor)
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        }

        @Override
        void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            LOGGER.debug "visitLocalVariable($descriptor)"
            usedPackages.addDescriptor(descriptor)
            super.visitLocalVariable(name, descriptor, signature, start, end, index)
        }

        @Override
        void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            LOGGER.debug "visitMultiANewArrayInsn($descriptor)"
            usedPackages.addDescriptor(descriptor)
            super.visitMultiANewArrayInsn(descriptor, numDimensions)
        }

        @Override
        AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            LOGGER.debug "visitTypeAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
        }

        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            LOGGER.debug "visitAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitAnnotation(descriptor, visible)
        }

        @Override
        AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            LOGGER.debug "visitInsnAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible)
        }

        @Override
        AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            LOGGER.debug "visitLocalVariablAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible)
        }

        @Override
        AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            LOGGER.debug "visitParameterAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitParameterAnnotation(parameter, descriptor, visible)
        }

        @Override
        AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            LOGGER.debug "visitTrayCatchAnnotation($descriptor)"
            usedPackages.addDescriptor(descriptor)
            return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)
        }
    }


    PackageUseScanner() {
        super(Opcodes.ASM9)
    }

    Set<String> getExternalPackages() {
        (usedPackages.packages - ownPackages.packages) as Set<String>
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        LOGGER.debug "Visiting $name : $superName"
        currentClassName = name
        ownPackages.addClass(name)
        if(superName) usedPackages.addClass(superName)
        for(intf in interfaces) {usedPackages.addClass(intf)}
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    void visitEnd() {
        LOGGER.debug "End visiting $currentClassName"
        currentClassName = null
        super.visitEnd()
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
        def mv =  super.visitMethod(access, name, descriptor, signature, exceptions)
        new ScannerMethodVisitor(mv)
    }

    @Override
    void visitInnerClass(String name, String outerName, String innerName, int access) {
        usedPackages.addClass(name)
        super.visitInnerClass(name, outerName, innerName, access)
    }

    @Override
    void visitNestHost(String nestHost) {
        LOGGER.debug "visitNestHost($nestHost)"
        usedPackages.addClass(nestHost)
        if(api >= Opcodes.ASM9) {
            super.visitNestHost(nestHost)
        }
    }

    @Override
    void visitNestMember(String nestMember) {
        LOGGER.debug "visitNestMember($nestMember)"
        usedPackages.addClass(nestMember)
        if(api >= Opcodes.ASM9) {
            super.visitNestMember(nestMember)
        }
    }

    @Override
    void visitOuterClass(String owner, String name, String descriptor) {
        usedPackages.addClass(owner)
        super.visitOuterClass(owner, name, descriptor)
    }

    @Override
    AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        usedPackages.addDescriptor(descriptor)
        return super.visitAnnotation(descriptor, visible)
    }

    @Override
    AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        usedPackages.addDescriptor(descriptor)
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
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

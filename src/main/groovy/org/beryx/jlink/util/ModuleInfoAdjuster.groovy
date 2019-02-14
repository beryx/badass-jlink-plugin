/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this dirOrJar except in compliance with the License.
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
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.Opcodes

import java.util.regex.Pattern

@CompileStatic
class ModuleInfoAdjuster {
    private static final Logger LOGGER = Logging.getLogger(ModuleInfoAdjuster.class);

    final File dirOrJar

    final String mergedModule
    final List<String> nonModularModules = []

    ModuleInfoAdjuster(String mergedModule, List<String> nonModularModules) {
        this.dirOrJar = dirOrJar
        this.mergedModule = mergedModule
        this.nonModularModules = nonModularModules
    }

    private static final Pattern MULTI_RELEASE_MODULE_INFO = ~'META-INF/versions/[0-9]+/module-info.class'
    /**
     * @return null if no adjustments have been made
     */
    Map<String, byte[]> getAdjustedDescriptors(File dirOrJar) {
        Map<String, byte[]> adjustedDescriptors = [:]
        if(nonModularModules) {
            boolean moduleInfoFound = false
            Util.scan(dirOrJar, { String basePath, String path, InputStream inputStream ->
                if((path == 'module-info.class') || path.matches(MULTI_RELEASE_MODULE_INFO)) {
                    moduleInfoFound = true

                    ClassReader cr = new ClassReader(inputStream)
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
                    def cv = new ModuleInfoClassVisitor(cw)
                    cr.accept(cv, 0)
                    if(cv.adjusted) {
                        adjustedDescriptors[path] = cw.toByteArray()
                    }
                }
            } as Closure)
            if(!moduleInfoFound) {
                LOGGER.info("No module-info.class found in: $dirOrJar")
            } else {
                if(adjustedDescriptors) {
                    LOGGER.info("Adjustments applied to module-info of $dirOrJar")
                } else {
                    LOGGER.debug("No adjustments needed for module-info of $dirOrJar")
                }
            }
        } else {
            LOGGER.debug("All artifacts are modular. No adjustments needed for module-info of $dirOrJar")
        }
        adjustedDescriptors
    }

    private class ModuleInfoClassVisitor extends ClassVisitor {
        boolean adjusted = false

        ModuleInfoClassVisitor(ClassWriter cw){
            super(Opcodes.ASM7, cw)
        }

        @Override
        ModuleVisitor visitModule(String name, int access, String version) {
            def defaultModuleVisitor = super.visitModule(name, access, version)
            new QualifiedModuleVisitor(defaultModuleVisitor)
        }

        private class QualifiedModuleVisitor extends ModuleVisitor {
            QualifiedModuleVisitor(ModuleVisitor defaultModuleVisitor) {
                super(Opcodes.ASM7, defaultModuleVisitor)
            }

            @Override
            void visitExport(String pkg, int access, String... modules) {
                if(modules && nonModularModules.any { modules.contains(it) }) {
                    adjusted = true
                    super.visitExport(pkg, access, modules + mergedModule)
                } else {
                    super.visitExport(pkg, access, modules)
                }
            }

            @Override
            void visitOpen(String pkg, int access, String... modules) {
                if(modules && nonModularModules.any { modules.contains(it) }) {
                    adjusted = true
                    super.visitOpen(pkg, access, modules + mergedModule)
                } else {
                    super.visitOpen(pkg, access, modules)
                }
            }
        }

    }
}

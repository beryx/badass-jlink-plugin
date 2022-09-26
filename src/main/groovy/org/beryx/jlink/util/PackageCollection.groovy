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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import groovyjarjarasm.asm.Type

@ToString
@CompileStatic
class PackageCollection {
    private static final Logger LOGGER = Logging.getLogger(PackageCollection.class);

    final TreeSet<String> packages = new TreeSet<String>()

    void addPackage(String pkg) {
        packages << adjust(pkg)
    }

    @CompileDynamic
    void addDescriptor(String descriptor) {
        addTypes(Type.getType(descriptor))
    }

    @CompileDynamic
    void addTypes(Type... types) {
        types.each { Type type ->
            switch(type.sort) {
                case Type.ARRAY: addTypes(type.elementType); break
                case Type.OBJECT: addClass(type.className); break;
                case Type.METHOD: addTypes(*type.argumentTypes, type.returnType); break;
            }
        }
    }

    void addClass(String className) {
        if(!className) return
        if(className.startsWith('[')) {
            addDescriptor(className)
        } else {
            def pkg = removeFromLastOccurrence(adjust(className), '.')
            if(pkg) packages << pkg
        }
    }

    static String removeFromLastOccurrence(String s, String delimiter) {
        int pos = s.lastIndexOf(delimiter)
        if(pos <= 0) {
            LOGGER.debug("Cannot remove from last occurrence of $delimiter in $s")
            return null
        }
        s.substring(0, pos)
    }

    static String adjust(String s) {
        s = s?.trim()
        if(!s) return ''
        while(!Character.isJavaIdentifierStart(s[0] as char)) {
            s = s.substring(1)
            if(!s) return ''
        }
        while(!Character.isJavaIdentifierPart(s[s.length() - 1] as char)) {
            s = s.substring(0, s.length()-1)
            if(!s) return ''
        }
        s.replace('/', '.')
    }
}

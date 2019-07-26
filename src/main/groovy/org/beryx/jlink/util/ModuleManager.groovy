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

import java.lang.module.ModuleDescriptor

@CompileStatic
class ModuleManager {
    // [module name : descriptor]
    final Map<String, ModuleDescriptor> moduleMap = [:]

    // [package : module name]
    final Map<String, String> exportMap = [:]

    ModuleManager(Object... modulePath) {
        Util.getJarsAndMods(modulePath).each {
            ModuleDescriptor md = Util.getModuleDescriptor(it)
            if(md) {
                moduleMap[md.name()] = md
                md.exports().each {exportMap[it.source()] = md.name()}
            }
        }
    }


}

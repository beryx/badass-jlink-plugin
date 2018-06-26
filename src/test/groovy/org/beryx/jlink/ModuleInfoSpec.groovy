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
package org.beryx.jlink

import org.beryx.jlink.impl.ModuleInfo
import spock.lang.Specification

class ModuleInfoSpec extends Specification {

    def "should create correct ModuleInfo"() {
        given:
        def mergedModule = {
            requires 'java.compiler'
            requires transitive 'jdk.unsupported'
            uses 'javax.annotation.processing.Processor'
            provides 'javax.tools.JavaCompiler' with 'org.eclipse.jdt.internal.compiler.tool.EclipseCompiler'
        }
        def moduleInfo = new ModuleInfo()
        mergedModule.resolveStrategy = Closure.DELEGATE_FIRST
        mergedModule.delegate = moduleInfo

        when:
        mergedModule()

        then:
        moduleInfo.requiresBuilders.size() == 2
        moduleInfo.usesBuilders.size() == 1
        moduleInfo.providesBuilders.size() == 1
        moduleInfo.toString(4) ==
            ''' |    requires java.compiler;
                |    requires transitive jdk.unsupported;
                |    uses javax.annotation.processing.Processor;
                |    provides javax.tools.JavaCompiler with org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;'''
                .stripMargin()
    }
}

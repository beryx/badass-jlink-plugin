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

import spock.lang.Specification
import spock.lang.Unroll

class JlinkTaskSpec extends Specification {
    @Unroll
    def "toModuleName(#name) should be #moduleName"() {
        expect:
        JlinkTask.toModuleName(name) == moduleName

        where:
        name | moduleName
        'a' | 'a'
        'org.xyz' | 'org.xyz'
        '?org.!!x+-yz!!=...' | 'org.x.yz'
    }

    @Unroll
    def "getModuleNameFrom(#text) should be #moduleName"() {
        expect:
        JlinkTask.getModuleNameFrom(text) == moduleName

        where:
        text                                                 | moduleName
        'module a.b.c{'                                      | 'a.b.c'
        '  \tmodule a.b.c\t { '                              | 'a.b.c'
        '/*my module*/\nmodule a.b.c {\n  exports a.b.c;\n}' | 'a.b.c'
    }
}

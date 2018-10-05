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


import org.beryx.jlink.util.Util
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class UtilSpec extends Specification {
    @Rule final TemporaryFolder tmpDir = new TemporaryFolder()

    @Unroll
    def "toModuleName(#name) should be #moduleName"() {
        expect:
        Util.toModuleName(name) == moduleName

        where:
        name | moduleName
        'a' | 'a'
        'org.xyz' | 'org.xyz'
        '?org.!!x+-yz!!=...' | 'org.x.yz'
    }

    @Unroll
    def "getModuleNameFrom(#text) should be #moduleName"() {
        expect:
        Util.getModuleNameFrom(text) == moduleName

        where:
        text                                                 | moduleName
        'module a.b.c{'                                      | 'a.b.c'
        'open module a.b.c{'                                 | 'a.b.c'
        '  \t  open\t  \tmodule \ta.b.c\t { '                | 'a.b.c'
        '/*my module*/\nmodule a.b.c {\n  exports a.b.c;\n}' | 'a.b.c'
    }

    @Unroll
    def "should get package #pkgName from jar entry #entry"() {
        expect:
        Util.getPackage(entry) == pkgName

        where:
        entry                            | pkgName
        'App.class'                      | null
        'org/App.class'                  | 'org'
        'org/example/App.class'          | 'org.example'
        'org/example-bad/App.class'      | null
        'org/example/info.txt'           | null
        'META-INF/org/example/App.class' | null
    }

    @Unroll
    def "should retrieve the correct dir for javaVersion=#javaVersion"() {
        when:
        new AntBuilder().unzip( src: "src/test/resources/libs/multi-release-hello.jar", dest: tmpDir.root.absolutePath )
        def dir = Util.getVersionedDir(tmpDir.root, javaVersion)

        then:
        dir?.name == expectedDir

        where:
        javaVersion | expectedDir
        8           | null
        9           | '9'
        10          | '9'
        11          | '11'
        12          | '11'
    }
}

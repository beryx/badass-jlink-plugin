/*
 * Copyright 2018 the original author or authors.
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
package org.beryx.jlink

import org.beryx.jlink.util.ModuleInfoAdjuster
import spock.lang.Specification
import spock.lang.TempDir

import java.lang.module.ModuleFinder
import java.nio.file.Path

class ModuleInfoAdjusterSpec extends Specification {
    @TempDir Path tmpDir

    def "should adjust module descriptors"() {
        given:
        def jarFile = new File('src/test/resources/libs/fake.java.desktop.jar')
        def adjuster = new ModuleInfoAdjuster('my.merged.module',
                                ['jdk.accessibility', 'jdk.jconsole'])

        when:
        def descriptor = adjuster.getAdjustedDescriptors(jarFile)['module-info.class']

        then:
        descriptor != null

        when:
        tmpDir.resolve('module-info.class') << descriptor
        def md = ModuleFinder.of(tmpDir).findAll().first().descriptor()
        def qExports = md.exports().findAll {it.qualified}.collect{[it.source(), it.targets()]} as Set
        def qOpens = md.opens().findAll {it.qualified}.collect{[it.source(), it.targets()]} as Set

        then:
        qExports == [
                ['sun.awt', ['jdk.unsupported.desktop', 'jdk.accessibility', 'my.merged.module'] as Set],
                ['java.awt.dnd.peer', ['jdk.unsupported.desktop'] as Set],
                ['sun.swing', ['jdk.unsupported.desktop'] as Set],
                ['sun.awt.dnd', ['jdk.unsupported.desktop'] as Set],
        ] as Set

        qOpens == [
                ['com.sun.java.swing.plaf.windows', ['jdk.jconsole', 'my.merged.module'] as Set],
                ['javax.swing.plaf.basic', ['jdk.jconsole', 'my.merged.module'] as Set],
        ] as Set
    }
}

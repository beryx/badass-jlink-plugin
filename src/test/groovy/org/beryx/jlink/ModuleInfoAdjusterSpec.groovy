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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.lang.module.ModuleFinder

class ModuleInfoAdjusterSpec extends Specification {
    @Rule final TemporaryFolder tmpDir = new TemporaryFolder()

    def "should adjust module descriptors"() {
        given:
        def jarFile = new File('src/test/resources/libs/fake.java.desktop.jar')
        def adjuster = new ModuleInfoAdjuster('my.merged.module',
                                ['jdk.accessibility', 'jdk.jconsole'])

        when:
        def descriptor = adjuster.getAdjustedDescriptor(jarFile)

        then:
        descriptor != null

        when:
        tmpDir.newFile('module-info.class').newOutputStream() << descriptor
        def md = ModuleFinder.of(tmpDir.root.toPath()).findAll().first().descriptor()
        def qExports = md.exports().findAll {it.qualified}.collect{it.toString()} as Set
        def qOpens = md.opens().findAll {it.qualified}.collect{it.toString()} as Set

        then:
        qExports == [
                'sun.awt to [jdk.unsupported.desktop, jdk.accessibility, my.merged.module]',
                'java.awt.dnd.peer to [jdk.unsupported.desktop]',
                'sun.swing to [jdk.unsupported.desktop]',
                'sun.awt.dnd to [jdk.unsupported.desktop]',
        ] as Set

        qOpens == [
                'com.sun.java.swing.plaf.windows to [jdk.jconsole, my.merged.module]',
                'javax.swing.plaf.basic to [jdk.jconsole, my.merged.module]',
        ] as Set
    }
}

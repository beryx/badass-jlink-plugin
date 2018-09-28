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

import org.beryx.jlink.util.ModuleManager
import spock.lang.Specification

class ModuleManagerSpec extends Specification {
    def "should correctly retrieve the names of the referenced packages"() {
        given:
        def moduleManager = new ModuleManager("src/test/resources/libs")

        when:
        def map = moduleManager.exportMap

        then:
        map['java.util.concurrent'] == 'java.base'
        map['javax.naming.directory'] == 'java.naming'
        map['org.xml.sax.helpers'] == 'java.xml'
        map['javax.xml.bind.annotation.adapters'] == 'java.xml.bind'
        map['ch.qos.logback.classic.net.server'] == 'ch.qos.logback.classic'
        map['ch.qos.logback.core.encoder'] == 'ch.qos.logback.core'
        map['org.slf4j.spi'] == 'org.slf4j'
    }
}

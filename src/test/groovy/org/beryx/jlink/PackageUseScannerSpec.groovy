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

import org.beryx.jlink.util.PackageUseScanner
import spock.lang.Specification

class PackageUseScannerSpec extends Specification {
    def "should correctly retrieve the names of the referenced packages"() {
        given:
        def scanner = new PackageUseScanner()
        def jarFile = new File('src/test/resources/libs/logback-core-1.3.0-alpha4.jar')

        when:
        def invalidEntries = scanner.scan(jarFile)
        if(invalidEntries) println "invalidEntries: $invalidEntries"

        then:
        !invalidEntries
        scanner.externalPackages == [
                'java.io',
                'java.lang',
                'java.lang.annotation',
                'java.lang.reflect',
                'java.net',
                'java.nio.channels',
                'java.nio.charset',
                'java.security',
                'java.security.cert',
                'java.sql',
                'java.text',
                'java.util',
                'java.util.concurrent',
                'java.util.concurrent.atomic',
                'java.util.concurrent.locks',
                'java.util.regex',
                'java.util.zip',
                'javax.mail',
                'javax.mail.internet',
                'javax.naming',
                'javax.net',
                'javax.net.ssl',
                'javax.servlet',
                'javax.servlet.http',
                'javax.sql',
                'javax.xml.parsers',
                'javax.xml.stream',
                'javax.xml.stream.events',
                'org.codehaus.commons.compiler',
                'org.codehaus.janino',
                'org.xml.sax',
                'org.xml.sax.helpers'
        ] as TreeSet<String>

    }
}

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

import org.beryx.jlink.util.ServiceProviderScanner
import spock.lang.Specification

class ServiceProviderScannerSpec extends Specification {
    def "should correctly detect provided services"() {
        given:
        def scanner = new ServiceProviderScanner()
        def jarFile = new File('src/test/resources/libs/ecj-3.13.102.jar')

        when:
        def invalidEntries = scanner.scan(jarFile)
        if(invalidEntries) println "invalidEntries: $invalidEntries"

        then:
        scanner.builders*.toString() as Set == [
                'provides javax.tools.JavaCompiler with org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;'
        ] as Set
    }
}

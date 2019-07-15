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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static org.beryx.jlink.data.ModuleInfo.ProvidesBuilder

@CompileStatic
class ServiceProviderScanner {
    private static final Logger LOGGER = Logging.getLogger(ServiceProviderScanner.class);
    final Set<ProvidesBuilder> builders = new HashSet<>()

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file, { String basePath, String path, InputStream inputStream ->
            if(path.startsWith('META-INF/services/')) {
                try {
                    def service = path - 'META-INF/services/'
                    if(service) {
                        def builder = new ProvidesBuilder(service)
                        inputStream.text.eachLine { line ->
                            line = line.trim()
                            int pos = line.indexOf('#')
                            if(pos >= 0) {
                                line = line.substring(0,pos).trim()
                            }
                            if(line) {
                                builder.with(line)
                            }
                        }
                        if(builder.implementations) {
                            builders << builder
                        }
                    }
                } catch (Exception e) {
                    LOGGER.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        } as Closure)
        invalidEntries
    }
}

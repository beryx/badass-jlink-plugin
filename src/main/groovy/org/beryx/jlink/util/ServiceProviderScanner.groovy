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

import org.gradle.api.Project
import static org.beryx.jlink.data.ModuleInfo.ProvidesBuilder

class ServiceProviderScanner {
    final Project project
    final Set<ProvidesBuilder> builders = new HashSet<>()

    ServiceProviderScanner(Project project) {
        this.project = project
    }

    List<String> scan(File file) {
        def invalidEntries = []
        Util.scan(file) { String basePath, String path, InputStream inputStream ->
            if(path.startsWith('META-INF/services/')) {
                try {
                    def service = path - 'META-INF/services/'
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
                } catch (Exception e) {
                    if(project) project.logger.info("Failed to scan $path", e)
                    invalidEntries << "${basePath}/${path}"
                }
            }
        }
        invalidEntries
    }
}

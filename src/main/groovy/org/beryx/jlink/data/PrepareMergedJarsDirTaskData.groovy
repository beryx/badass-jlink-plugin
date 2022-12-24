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
package org.beryx.jlink.data

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.artifacts.Configuration

@CompileStatic
@ToString(includeNames = true)
class PrepareMergedJarsDirTaskData extends BaseTaskData {
    List<String> forceMergedJarPrefixes
    List<String> extraDependenciesPrefixes
    File mergedJarsDir
    String jlinkJarsDirPath
    String nonModularJarsDirPath
    String tmpJarsDirPath
    Configuration configuration
    String javaHome
    int jvmVersion
    Map<String,List<String>> jarExcludes
}

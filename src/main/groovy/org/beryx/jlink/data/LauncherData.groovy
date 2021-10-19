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
import org.beryx.jlink.util.Util
import org.gradle.api.Project

@CompileStatic
@ToString(includeNames = true)
class LauncherData implements Serializable {
    private static final List<String> UNDEFINED_ARGS = ['<UNDEFINED>']

    List<String> args = UNDEFINED_ARGS
    List<String> jvmArgs = UNDEFINED_ARGS

    String name
    File unixScriptTemplate
    File windowsScriptTemplate
    boolean noConsole

    LauncherData(String name) {
        this.name = name
    }

    List<String> getEffectiveArgs(Project project) {
        (args != UNDEFINED_ARGS) ? args : Util.getDefaultArgs(project)
    }

    List<String> getEffectiveJvmArgs(Project project) {
        (jvmArgs != UNDEFINED_ARGS) ? jvmArgs : Util.getDefaultJvmArgs(project)
    }
}

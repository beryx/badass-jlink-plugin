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

import org.beryx.jlink.data.LauncherData
import org.beryx.jlink.util.LaunchScriptGenerator
import spock.lang.Specification
import spock.lang.Unroll

class LaunchScriptGeneratorSpec extends Specification {
    @Unroll
    def "should generate the correct unix script with jvmArgs=#jvmArgs and args=#args"() {
        when:
        LauncherData ld = new LauncherData()
        ld.name = 'hello'
        ld.args = args
        ld.jvmArgs = jvmArgs
        def generator = new LaunchScriptGenerator('org.example.hello', 'org.example.Hello', ld)
        def scriptLines = generator.getScript(LaunchScriptGenerator.Type.UNIX).readLines()

        then:
        scriptLines[0] == '#!/bin/sh'
        scriptLines[1] == 'DIR="${0%/*}"'
        scriptLines[2].replace('  ', ' ') == lastLine

        where:
        jvmArgs                      | args                         | lastLine
        []                           | []                           | '"$DIR/java" -p "$DIR/../app" -m org.example.hello/org.example.Hello "$@"'
        ['-Xmx200m']                 | ['Alice']                    | '"$DIR/java" -Xmx200m -p "$DIR/../app" -m org.example.hello/org.example.Hello Alice "$@"'
        ['-Xmx200m', '-Ddebug=true'] | ['Alice', 'Bob']             | '"$DIR/java" -Xmx200m -Ddebug=true -p "$DIR/../app" -m org.example.hello/org.example.Hello Alice Bob "$@"'
        ['-cp', '{{BIN_DIR}}/data']  | ['{{BIN_DIR}}/../names.txt'] | '"$DIR/java" -cp "$DIR/data" -p "$DIR/../app" -m org.example.hello/org.example.Hello "$DIR/../names.txt" "$@"'
    }

    @Unroll
    def "should generate the correct windows script with jvmArgs=#jvmArgs and args=#args"() {
        when:
        LauncherData ld = new LauncherData()
        ld.name = 'hello'
        ld.args = args
        ld.jvmArgs = jvmArgs
        def generator = new LaunchScriptGenerator('org.example.hello', 'org.example.Hello', ld)
        def scriptLines = generator.getScript(LaunchScriptGenerator.Type.WINDOWS).readLines()

        then:
        scriptLines[0] == '@echo off'
        scriptLines[1] == 'set DIR="%~dp0"'
        scriptLines[2] == 'set JAVA_EXEC="%DIR:"=%\\java"'
        scriptLines[3].replace('  ', ' ') == lastLine

        where:
        jvmArgs                      | args                         | lastLine
        []                           | []                           | 'pushd %DIR% & %JAVA_EXEC% -p "%~dp0/../app" -m org.example.hello/org.example.Hello %* & popd'
        ['-Xmx200m']                 | ['Alice']                    | 'pushd %DIR% & %JAVA_EXEC% -Xmx200m -p "%~dp0/../app" -m org.example.hello/org.example.Hello Alice %* & popd'
        ['-Xmx200m', '-Ddebug=true'] | ['Alice', 'Bob']             | 'pushd %DIR% & %JAVA_EXEC% -Xmx200m -Ddebug=true -p "%~dp0/../app" -m org.example.hello/org.example.Hello Alice Bob %* & popd'
        ['-cp', '{{BIN_DIR}}/data']  | ['{{BIN_DIR}}/../names.txt'] | 'pushd %DIR% & %JAVA_EXEC% -cp "%~dp0/data" -p "%~dp0/../app" -m org.example.hello/org.example.Hello "%~dp0/../names.txt" %* & popd'
    }
}

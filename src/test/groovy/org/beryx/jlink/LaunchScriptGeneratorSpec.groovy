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
        LauncherData ld = new LauncherData('hello')
        ld.args = args
        ld.jvmArgs = jvmArgs
        def generator = new LaunchScriptGenerator('org.example.hello', 'org.example.Hello', ld, jvmArgs, args)
        def scriptLines = generator.getScript(LaunchScriptGenerator.Type.UNIX).readLines()

        then:
        scriptLines[0] == '#!/bin/sh'
        scriptLines[4] == 'DIR="${0%/*}"'
        def actual = scriptLines[-1].replace('  ', ' ')
        if(actual != lastLine) {
            println "[DEBUG_LOG] UNIX Actual:   $actual"
            println "[DEBUG_LOG] UNIX Expected: $lastLine"
        }
        actual == lastLine

        where:
        jvmArgs                      | args                         | lastLine
        []                           | []                           | 'exec "$DIR/java" $CDS_JVM_OPTS -p "$DIR/../app" -m org.example.hello/org.example.Hello "$@"'
        ['-Xmx200m']                 | ['Alice']                    | 'exec "$DIR/java" $CDS_JVM_OPTS -Xmx200m -p "$DIR/../app" -m org.example.hello/org.example.Hello Alice "$@"'
        ['-Xmx200m', '-Ddebug=true'] | ['Alice', 'Bob']             | 'exec "$DIR/java" $CDS_JVM_OPTS -Xmx200m -Ddebug=true -p "$DIR/../app" -m org.example.hello/org.example.Hello Alice Bob "$@"'
        ['-cp', '{{BIN_DIR}}/data']  | ['{{BIN_DIR}}/../names.txt'] | 'exec "$DIR/java" $CDS_JVM_OPTS -cp "$DIR/data" -p "$DIR/../app" -m org.example.hello/org.example.Hello "$DIR/../names.txt" "$@"'
    }

    @Unroll
    def "should generate the correct windows script with jvmArgs=#jvmArgs and args=#args"() {
        when:
        LauncherData ld = new LauncherData('hello')
        ld.args = args
        ld.jvmArgs = jvmArgs
        def generator = new LaunchScriptGenerator('org.example.hello', 'org.example.Hello', ld, jvmArgs, args)
        def scriptLines = generator.getScript(LaunchScriptGenerator.Type.WINDOWS).readLines()

        then:
        scriptLines[0] == '@echo off'
        scriptLines[1] == 'set "DIR=%~dp0"'
        scriptLines[2] == 'set "JAVA_EXEC=%DIR%java.exe"'
        def pushdIndex = scriptLines.lastIndexOf('pushd %DIR%')
        assert pushdIndex >= 0
        def actualBlock = scriptLines[pushdIndex..<(pushdIndex + 5)]*.replace('  ', ' ')
        def expectedBlock = [
                'pushd %DIR%',
                commandLine,
                'set EXITCODE=%ERRORLEVEL%',
                'popd',
                'exit /b %EXITCODE%'
        ]
        if(actualBlock != expectedBlock) {
            println "[DEBUG_LOG] WIN Actual block:   $actualBlock"
            println "[DEBUG_LOG] WIN Expected block: $expectedBlock"
        }
        actualBlock == expectedBlock

        where:
        jvmArgs                      | args                         | commandLine
        []                           | []                           | '%JAVA_EXEC% %CDS_JVM_OPTS% -p "%~dp0/../app" -m org.example.hello/org.example.Hello %*'
        ['-Xmx200m']                 | ['Alice']                    | '%JAVA_EXEC% %CDS_JVM_OPTS% -Xmx200m -p "%~dp0/../app" -m org.example.hello/org.example.Hello Alice %*'
        ['-Xmx200m', '-Ddebug=true'] | ['Alice', 'Bob']             | '%JAVA_EXEC% %CDS_JVM_OPTS% -Xmx200m -Ddebug=true -p "%~dp0/../app" -m org.example.hello/org.example.Hello Alice Bob %*'
        ['-cp', '{{BIN_DIR}}/data']  | ['{{BIN_DIR}}/../names.txt'] | '%JAVA_EXEC% %CDS_JVM_OPTS% -cp "%~dp0/data" -p "%~dp0/../app" -m org.example.hello/org.example.Hello "%~dp0/../names.txt" %*'
    }
}

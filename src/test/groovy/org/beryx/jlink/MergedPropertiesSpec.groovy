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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class MergedPropertiesSpec extends AbstractJlinkPluginTest {
    def "should merge properties files with the same path from different non-modular jars"() {
        when:
        setUpBuild('merged-properties')
        def libDir = new File(testProjectDir.toFile(), 'lib')
        // no trailing line break, to verify that the merged entries are not joined
        createJar(new File(libDir, 'props-a-1.0.jar'), 'props.a', [
                'extension.properties': 'factory.a=A\nshared=from-a',
                'identical.properties': 'key=value\n'
        ])
        createJar(new File(libDir, 'props-b-1.0.jar'), 'props.b', [
                'extension.properties': 'factory.b=B\nshared=from-b\n',
                'identical.properties': 'key=value\n'
        ])
        BuildResult result = runGradleWithLockRetry(GradleRunner.create()
                .withDebug(false)
                .withGradleVersion('8.14.4')
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(JlinkPlugin.TASK_NAME_JLINK, "-is"))

        then:
        checkOutput(result, 'propsHello', '{factory.a=A, factory.b=B, shared=from-b}')
        result.output.contains('Merging extension.properties from props-b-1.0.jar into the merged module: conflicting values for [shared], using the values from props-b-1.0.jar.')
        new File(testProjectDir.toFile(), 'build/jlinkbase/mergedjars/identical.properties').text == 'key=value\n'
    }

    private static void createJar(File jarFile, String automaticModuleName, Map<String, String> entries) {
        jarFile.parentFile.mkdirs()
        def manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, '1.0')
        manifest.mainAttributes.putValue('Automatic-Module-Name', automaticModuleName)
        new JarOutputStream(jarFile.newOutputStream(), manifest).withCloseable { out ->
            entries.each { name, text ->
                out.putNextEntry(new JarEntry(name))
                out.write(text.getBytes('ISO-8859-1'))
                out.closeEntry()
            }
        }
    }
}

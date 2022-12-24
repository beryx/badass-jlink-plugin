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

import org.beryx.jlink.data.JlinkPluginExtension
import org.beryx.jlink.util.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

import static org.beryx.jlink.util.Util.EXEC_EXTENSION

class BaseTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(BaseTask.class);

    @Internal
    final JlinkPluginExtension extension

    BaseTask() {
        this.extension = (JlinkPluginExtension)project.extensions.getByName(JlinkPlugin.EXTENSION_NAME)
        group = 'build'
    }

    @Input
    String getJlinkBasePath() {
        extension.jlinkBasePath.get()
    }

    @Internal
    String getJavaHomeOrDefault() {
        return extension.javaHome.present ? extension.javaHome.get() : defaultJavaHome
    }

    @Internal
    String getDefaultJavaHome() {
        def value = System.properties['badass.jlink.java.home']
        if(value) return value
        value = System.getenv('BADASS_JLINK_JAVA_HOME')
        if(value) return value
        value = Util.getDefaultToolchainJavaHome(project)
        if(value) return value
        value = System.properties['java.home']
        if(['javac', 'jar', 'jlink'].every { new File("$value/bin/$it$EXEC_EXTENSION").file }) return value
        return System.getenv('JAVA_HOME')
    }

    @Internal
    String getDefaultMainClass() {
        def mainClass = defaultMainClassModern
        if(!mainClass) return defaultMainClassLegacy
        def mainModule = defaultModuleModern
        def moduleName = extension.moduleName.get()
        if(mainModule != moduleName) {
            LOGGER.warn("The module name specified in 'application.mainModule' ($mainModule) has not the expected value ($moduleName).")
        }
        mainClass
    }

    @Internal
    String getDefaultMainClassModern() {
        try {
            return project.application?.mainClass?.get() as String
        } catch (Exception e) {
            return null
        }
    }

    @Internal
    String getDefaultModuleModern() {
        try {
            return project.application?.mainModule?.get() as String
        } catch (Exception e) {
            return null
        }
    }

    @Internal
    String getDefaultMainClassLegacy() {
        def mainClass = project['mainClassName'] as String
        if(!mainClass) throw new GradleException("mainClass not configured")
        int pos = mainClass.lastIndexOf('/')
        if(pos < 0) return mainClass
        def mainClassModule = mainClass.substring(0, pos)
        def moduleName = extension.moduleName.get()
        if(mainClassModule != moduleName) {
            LOGGER.warn("The module name specified in 'mainClassName' ($mainClassModule) has not the expected value ($moduleName).")
        }
        mainClass.substring(pos + 1)
    }
}

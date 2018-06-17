package org.beryx.jlink

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

class JlinkPluginExtension {
    final Property<String> jlinkInputProperty
    final RegularFileProperty jlinkOutputProperty

    JlinkPluginExtension(Project project) {
        jlinkInputProperty = project.objects.property(String)
        jlinkInputProperty.set('jlinkInputProperty-default-val')
        jlinkOutputProperty = project.layout.fileProperty()
        jlinkOutputProperty.set(project.file("$project.buildDir/jlink/jlink-out.txt"))
    }
}

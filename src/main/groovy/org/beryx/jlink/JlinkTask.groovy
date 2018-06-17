package org.beryx.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.beryx.jlink.impl.JlinkTaskImpl

class JlinkTask extends DefaultTask {
    @Input @Optional
    Property<String> jlinkInputProperty

    @OutputFile @Optional
    RegularFileProperty jlinkOutputProperty

    @TaskAction
    void jlinkTaskAction() {
        def input = jlinkInputProperty.get()
        def output = jlinkOutputProperty.get().asFile
        new JlinkTaskImpl(input, output).execute()
    }
}

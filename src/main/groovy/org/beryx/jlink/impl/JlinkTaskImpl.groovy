package org.beryx.jlink.impl

class JlinkTaskImpl {
    String jlinkInputProperty
    File jlinkOutputProperty

    JlinkTaskImpl(String sampleInputProperty, File jlinkOutputProperty) {
        this.jlinkInputProperty = sampleInputProperty
        this.jlinkOutputProperty = jlinkOutputProperty
    }

    void execute() {
        jlinkOutputProperty.text = "jlink: jlinkInputProperty = $jlinkInputProperty"
    }
}

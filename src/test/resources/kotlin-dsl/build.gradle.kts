import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.beryx.jlink")
}

repositories {
    mavenCentral()
}

extra["moduleName"] = "org.example.kotlin"

application {
    mainClass.set("org.example.kotlin.Hello")
}

jlink {
    // Use platform-appropriate icon
    val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
    val iconFile = when {
        currentOs.isMacOsX -> project.file("assets/icon.icns")
        currentOs.isWindows -> project.file("assets/icon.ico")
        else -> project.file("assets/icon.png") // use PNG for Linux
    }
    if (iconFile.exists()) {
        jpackage.imageOptions.addAll("--icon", iconFile.absolutePath)
    }

    launcher {
        name = "hello"
        noConsole = false
        jvmArgs = listOf(
            "-Xms512m",
            "-Xmx4g",
            "-XX:+UseShenandoahGC"
        )
    }
    secondaryLauncher {
        name = "helloAgain"
        mainClass = "org.example.kotlin.HelloAgain"
    }
    secondaryLauncher {
        name = "howdy"
        moduleName= "org.example.kotlin"
        mainClass = "org.example.kotlin.Howdy"
    }
}

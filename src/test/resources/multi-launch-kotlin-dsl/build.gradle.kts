plugins {
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.beryx.jlink")
}

repositories {
    mavenCentral()
}

extra["moduleName"] = "org.example.multi"

application {
    mainClass.set("org.example.multi.Hello")
}

jlink {
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
        mainClass = "org.example.multi.HelloAgain"
    }
    secondaryLauncher {
        name = "howdy"
        moduleName= "org.example.multi"
        mainClass = "org.example.multi.Howdy"
    }
}

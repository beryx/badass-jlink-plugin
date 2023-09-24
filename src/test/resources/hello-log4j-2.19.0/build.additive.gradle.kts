plugins {
    id("org.beryx.jlink")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val log4jVersion = "2.19.0"

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.5")
    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.5")
}

application {
    mainClass.set("org.beryx.modular.hello.Hello")
}

val compileJava: JavaCompile by tasks
compileJava.options.compilerArgs = listOf("--module-path", compileJava.classpath.asPath)
compileJava.classpath = files()

jlink {
    launcher {
        name = "hello"
    }
    mergedModule {
        excludeRequires("java.compiler", "java.rmi")
        excludeUses("org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory")
        excludeProvides(mapOf("servicePattern" to "org.apache.logging.*"))
    }
}

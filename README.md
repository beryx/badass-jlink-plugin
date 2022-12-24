[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/beryx/badass-jlink-plugin/blob/master/LICENSE)
[![Build Status](https://img.shields.io/github/workflow/status/beryx/badass-jlink-plugin/Java%2011%20Gradle%20CI)](https://github.com/beryx/badass-jlink-plugin/actions?query=workflow%3A%22Java+11+Gradle+CI%22)

## Badass JLink Plugin ##

Using this Gradle plugin you can create a custom runtime image of your modular application with minimal effort, 
even if it depends on automatic modules.
The plugin also lets you create an application installer with the [jpackage](https://jdk.java.net/jpackage/) tool introduced in Java 14
(see [fxgl-sliding-puzzle](https://github.com/beryx/fxgl-sliding-puzzle/blob/master/README.adoc) for a usage example).

:bulb: For non-modular applications use the [Badass-Runtime plugin](https://badass-runtime-plugin.beryx.org/releases/latest/).

Badass-JLink exposes an extension with the name `jlink` to let you configure various
aspects of its operation.
A simple example configuration is shown below:

```
jlink {
    options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
    launcher{
        name = 'hello'
        jvmArgs = ['-Dlog4j.configurationFile=./log4j2.xml']
    }
}
``` 

The following projects illustrate how to use this plugin to create custom runtime images:
- [badass-jlink-example](https://github.com/beryx-gist/badass-jlink-example) - a 'Hello world' application using slf4j and logback.
- [badass-jlink-example-log4j2-javafx](https://github.com/beryx-gist/badass-jlink-example-log4j2-javafx) - a 'Hello world' JavaFX application using log4j2.
- [badass-jlink-example-javafx-multiproject](https://github.com/beryx-gist/badass-jlink-example-javafx-multiproject) - A 'Hello world' JavaFX application with a Gradle multi-project build.
- [badass-jlink-example-richtextfx](https://github.com/beryx-gist/badass-jlink-example-richtextfx) - A [RichTextFX](https://github.com/FXMisc/RichTextFX) project that shows how to configure splash screens and file associations when using jpackage.
- [badass-jlink-example-kotlin-javafx](https://github.com/beryx-gist/badass-jlink-example-kotlin-javafx) - a 'Hello world' JavaFX application written in Kotlin.
- [badass-jlink-example-kotlin-tornadofx](https://github.com/beryx-gist/badass-jlink-example-kotlin-tornadofx) - a 'Hello world' application written in Kotlin using [tornadofx](https://github.com/edvin/tornadofx).
- [badass-jlink-spring-petclinic](https://github.com/beryx-gist/badass-jlink-spring-petclinic) - creates a custom runtime image of the [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) application.
- [fxgl-sliding-puzzle](https://github.com/beryx/fxgl-sliding-puzzle/blob/master/README.adoc) - a sliding puzzle game using the [FXGL](https://github.com/AlmasB/FXGL) library.
- [copper-modular-demo](https://github.com/copper-engine/copper-modular-demo) - creates a custom runtime image of a [COPPER 5](http://copper-engine.org/) modular application. 
- [javafx-jlink-starter-gradle](https://github.com/brunoborges/javafx-jlink-starter-gradle) - A JavaFX starter project with an Azure Pipelines setup that produces binaries for Windows, Mac, and Linux.
- [javafx-springboot-badass-jlink](https://github.com/mockbirds/javafx-springboot-badass-jlink) - A modular JavaFX application with Spring Boot.
- [pdf-decorator](https://bitbucket.org/walczak_it/pdf-decorator/src/master/) - A [JavaFX tool](http://pdf-decorator.walczak.it/) to add stamps and backgrounds to PDF documents. There's also a [related article](https://walczak.it/blog/distributing-javafx-desktop-applications-without-requiring-jvm-using-jlink-and-jpackage) explaining how to migrate to this plugin. 

### This is a complex plugin. Please [read the documentation](https://badass-jlink-plugin.beryx.org/releases/latest/) before using it.

See the [list of all releases](https://github.com/beryx/badass-jlink-plugin/blob/gh-pages/releases.md) if you use an older version of this plugin. 

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/beryx/badass-jlink-plugin/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/beryx/badass-jlink-plugin/master.svg?label=Build)](https://travis-ci.org/beryx/badass-jlink-plugin)

## Badass JLink Plugin ##

Using this Gradle plugin you can create a custom runtime image of your modular application with minimal effort, 
even if it depends on automatic modules. 

:bulb: For non-modular applications use the https://badass-runtime-plugin.beryx.org/releases/latest/[Badass-Runtime plugin].

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
- [badass-jlink-example-kotlin-javafx](https://github.com/beryx-gist/badass-jlink-example-kotlin-javafx) - a 'Hello world' JavaFX application written in Kotlin.
- [badass-jlink-example-kotlin-tornadofx](https://github.com/beryx-gist/badass-jlink-example-kotlin-tornadofx) - a 'Hello world' application written in Kotlin using [tornadofx](https://github.com/edvin/tornadofx).
- [badass-jlink-spring-petclinic](https://github.com/beryx-gist/badass-jlink-spring-petclinic) - creates a custom runtime image of the [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) application.
- [copper-modular-demo](https://github.com/copper-engine/copper-modular-demo) - creates a custom runtime image of a [COPPER 5](http://copper-engine.org/) modular application. 

### This is a complex plugin. Please [read the documentation](https://badass-jlink-plugin.beryx.org/releases/latest/) before using it.

See the [list of all releases](https://github.com/beryx/badass-jlink-plugin/blob/gh-pages/releases.md) if you use an older version of this plugin. 

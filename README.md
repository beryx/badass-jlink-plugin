[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/beryx/badass-jlink-plugin/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/beryx/badass-jlink-plugin/master.svg?label=Build)](https://travis-ci.org/beryx/badass-jlink-plugin)

## Badass JLink Plugin ##

A Gradle plugin that provides a `jlink` task, which can be configured using the `jlink` extension.

Example `build.gradle` configuration:

```
plugins {
    id 'org.beryx.jlink' version '1.0.1'
}

jlink {
    launcherName = 'hello'
    mergedModule {
        requires 'java.naming';
        requires 'java.xml';
        requires 'java.xml.bind';
    }
}
```

Check [this simple application](https://github.com/beryx-gist/badass-jlink-example), which shows how to use the plugin to create a custom runtime image.

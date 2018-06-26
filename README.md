## Badass JLink Plugin ##

A Gradle plugin that provides a `jlink` task, which can be configured using the `jlink` extension.

Example `build.gradle` configuration:

```
plugins {
    id 'org.beryx.jlink' version '1.0.0'
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

## Badass Jlink Plugin ##

A Gradle plugin that provides a `jlink` task, which can be configured using the `jlink` extension.

The current task implementation reads the value of `jlinkInputProperty` and writes it into the file specified by `jlinkOutputProperty`.

Example `build.gradle` configuration:

```
plugins {
    id 'org.beryx.jlink' version '1.0.0'
}

jlink {
    jlinkInputProperty = 'my-input-value'
    jlinkOutputProperty = file('my-output-file.txt')
}
```

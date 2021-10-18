# Guides and tricks on the SAVE development

Small guide that contain information and HOW-TOs

## Building
The project is built using gradle. To produce an executable, execute the following
(the task is an alias, which is resolved depending on the current platform):
```bash
$ ./gradlew :save-cli:linkReleaseExecutableMultiplatform
```
The compiled binary resides in the following location: `save-cli/build/bin/<target>/<mode>Executable/save-<version>.[k]exe`

To speed up local development we advise to add the following properties into [gradle.properties](gradle.properties):
```properties
reckon.stage=snapshot
detekt.multiplatform.disabled=true
disableRedundantTargets=true
```
Setting project version to snapshot allows gradle to cache stuff more effectively. Disabling detekt (see [Code style] section below)
reduces build time, and checks can be executed separately on during CI. Finally, `disableRedundantTargets` disables cross-compilations
and leaves only default target for current platform.

To build all subprojects and run tests, you can simply execute
```bash
$ ./gradlew build
```

### Important
There could be some problems in the resolving of dependencies in IDEA due to a weak Kotlin gradle multiplatform support. 

### save-cli and adding new cli options
To add a new cli option to save simply add this option to `buildSrc/src/main/resources/config-options.json`. Once the project
is built, it will be added to [OptionsTable.md](OptionsTable.md) and to the generated `SaveProperties.kt` file.

### save toml configuration options

### Code style
Code style and code smells are checked using [diktat](https://github.com/cqfn/diktat) and [detekt](https://github.com/detekt/detekt).

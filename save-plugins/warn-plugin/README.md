# SAVE warn plugin
Plugin that runs the provided executable and compares emitted warnings with expected; expected warnings are set in the same input files.
Please note, that it is important for test resources to have specific postfixes. For test file it should be `Test`.

## Source files
Test source files (input for SAVE) should have a comment line (use single-line commenting syntax of the target programming language for it)
with a warning in the following format: `;warn:$line:$column: Warning text`. Note, that this warning can be put into any place of the code.
Also note, that if your warning text does not contain line or column - you can disable it by the following code in `save.toml`:
```toml
warningTextHasColumn = false
warningTextHasLine = false
```

If `ignore-save-comments` is set to `true` in `save.properties`, than line numbers are determined skipping the lines with warning markers, i.e.
```java
// this is line 1
// ;warn:2:1: This will trigger on line 3
// this is line 3, but will be treated as line 2
```

## Configuration
Assuming you want to run your tool on input file `path/to/example/ExampleTest1.kt`,
and you have directory structure like this
```bash
build.gradle.kts
save.properties
src/main/kotlin
src/test/resources
| save.toml
| path
  | to
    | example1
      | ExampleTest1.kt
      | ExampleTest2.kt
    | example2
    ...
```
and the content of the file `ExampleTest1.kt`:
```kotlin
// ;warn:1:7: Class name should be in an uppercase format
// ;warn:3:13: Method B() should follow camel-case convention 
class a {
    // ;warn:2:13: Single symbol variables are not informative
    // ;warn:2:14: Trailing semicolon is redundant in Kotlin
     val b: String;
     fun B(): String {}
     fun setB(): String {}
}
```

you will need the following SAVE configuration:

`save.toml`:
```toml
[general]
description = "My suite description"
suiteName = "DocsCheck"

[warn]
execCmd = "./detekt --build-upon-default-config -i"
warningsInputPattern = "// ;warn:(\\d+):(\\d+): (.*)"  # warning is set inside the comment in code, `//` marks comment start in Java
warningsOutputPattern = "\\w+ - (\\d+)/(\\d+) - (.*)$"  # e.g. `WARN - 10/14 - Class name is in incorrect case`
lineCaptureGroup = 1
columnCaptureGroup = 2
messageCaptureGroup = 3
warningTextHasColumn = true
warningTextHasLine = true
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all files
matching `inputFilePattern`. It will then execute `$exec_cmd $testFile` (since we specified
`batchMode = false`, it will provide inputs one by one) and compare warnings its stdout (as per `output` option) parsed using `warningsOutputPattern` with warnings
parsed from the same `$testFile` using `warningsInputPattern`.

`warningsInputPattern` and `warningsOutputPattern` must include some mandatory capture groups: for line number (if `warningTextHasLine` is true),
for column number (if `warningTextHasColumn` is true) and for warning text. Their indices can be customized
with `lineCaptureGroup`, `columnCaptureGroup` and `messageCaptureGroup` parameters. These parameters are shared between input and output pattern;
usually you'll want them to be consistent to make testing easier, i.e. if input has line number, then so should output.
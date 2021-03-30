## Save warn plugin

Plugin that runs the provided executable and compares emitted warnings with expected; expected warnings are set in the same input files.
Please note, that it is important for test resources to have specific postfixes. For test file it should be `Test`.

## Source files
Test source files (input for save) should have a comment line (use single-line commenting syntax of the target programming language for it)
with a warning in the following format: `;warn:$line:$column: Warning text`. Note, that this warning can be put into any place of the code.
Also note, that if your warning text does not contain line or column - you can disable it by the following code in `save.toml`:
```toml
wanrningTextHasColumn = false
wanrningTextHasLine = false
```

## Configuraton
Assuming you want to run your tool on input file path/to/example/ExampleTest1.kt,
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

Example:

```kotlin
// ;warn:1:7: Class name should be in an uppercase format
// ;warn:3:13: Method B() should follow camel-case convention 
class a {
    // ;warn:2:13: Single symbol variables are not informative
     String b;
     String B() {}
     String setB() {}
}
```

you will need the following SAVE configuration:

`save.toml`:
```toml
[general]
description = "My suite description"
suiteName = "DocsCheck"

[warn]
execCmd="./detekt --build-upon-default-config -i"
warningsOutputPattern = \w+ - \d+/\d+ - .*$
output = stdout  # you can also use 'file' here to do fixes right into the test file (test files won't be broken or changed)
# FixMe: what to do with a file configuration? Do we need to have an extra option with a path to this file?
batchMode = false
wanrningTextHasColumn = true
wanrningTextHasLine = true
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all files
matching `inputFilePattern`. It will then execute `$exec_cmd $testFile` (since we specified
`batchMode = false`, it will provide inputs one by one) and compare warnings its stdout (as per `output` option) parsed using `warningsOutputPattern` with warnings
parsed from the same `$testFile` using `warningsInputPattern`.
Results will be written in plain text as well as JSON.
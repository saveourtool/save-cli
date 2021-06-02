## Save fix plugin
Plugin that runs provided executable on the initial file with a test source code and compares its output with an expected result.
Please note, that it is important for test resources to have specific postfixes. By the default test file it should be `Test`
, for the file with expected result - it should be `Expected`.

// FixMe: say some words about the configuration of postfixes

## Configuration
Assuming you want to run your tool on input file path/to/example1/ExampleTest.kt and compare with /path/to/example1/ExampleExpected.kt,
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
      | ExampleTest.kt
      | ExampleExpected.kt
      | Example2Test.kt
      | Example2Expected.kt
    | example2
    ...
```
you will need the following SAVE configuration:

`save.properties`:
```properties
rootDir=src/test/resources
reports=plain,json
reportsDir=build/reports/save
mode=parallel
language=kotlin
```

`save.toml`:
```toml
[general]
description = "My suite description"
suiteName = "DocsCheck"

[diff]
exec_cmd="./ktlint -R diktat-0.4.2.jar -F"
testFilePattern="*Test.kt"
expectedFilePattern="*Expected.kt"
batchMode = false
resourceNameTestSuffix = Test
resourceNameExpectedSuffix = Expected
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all pairs of files
matching `testFilePattern` and `expectedFilePattern` with same prefix. It will then execute `$exec_cmd $testFile` (since we specified
`batchMode = false`, it will provide inputs one by one) and compare its stdout (as per `output` option) with respecting `$expectedFile`.
Results will be written in plain text as well as JSON.
`resourceNameTestSuffix` must include suffix name of the test file. `resourceNameExpectedSuffix` must include suffix name of the expected file.

Plugin that runs the provided executable and compares emitted warnings with expected; expected warnings are set in the same input files.

Assuming you want to run your tool on input file path/to/example/Example1.kt,
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
      | Example1.kt
      | Example2.kt
    | example2
    ...
```

The input files should look like this:
```kotlin
// warn:1:6: Class name should be in an uppercase format
class a {
     String b;
     String getB() {}
     String setB() {}
}
```

you will need the following SAVE configuration:

`save.properties`:
```properties
exec_cmd="./detekt --build-upon-default-config -i"
rootDir=src/test/resources
reports=plain,json
reportsDir=build/reports/save
mode=parallel
language=kotlin
```

`save.toml`:
```toml
[test]
name = detekt tests
category = static analysis

[diff]
testSuites = possible-bugs
inputFilePattern=*.kt
warningsInputPattern = ".*$filename.format:$line:$column:.*"
warningsOutputPattern = \w+ - \d+/\d+ - .*$
output = stdout
batchMode = false
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all files
matching `inputFilePattern`. It will then execute `$exec_cmd $testFile` (since we specified
`batchMode = false`, it will provide inputs one by one) and compare warnings its stdout (as per `output` option) parsed using `warningsOutputPattern` with warnings
parsed from the same `$testFile` using `warningsInputPattern`.
Results will be written in plain text as well as JSON.
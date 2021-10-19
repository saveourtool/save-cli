# SAVE warn plugin
Plugin that runs the provided executable and compares emitted warnings with expected; expected warnings are set in the same input files.
Please note, that it is important for test resources to have specific keywords. For test file it should be `Test`.

### Examples
If you don't like to read long readme file, you can simply check [examples](/examples/kotlin-diktat/warn).

### Source files
Test source files (input for SAVE) should have a comment line (use single-line commenting syntax of the target programming language for it)
with a warning in the following format: `;warn:$line:$column: Warning text`. Note, that this warning can be put into any place of the code.
Also note, that if your warning text does not contain line or column - you can disable it by the following code in `save.toml`:
```toml
warningTextHasColumn = false
warningTextHasLine = false
```

If `exactWarningsMatch` is set to `true` in `save.toml`, then an exact match of expected and actual warnings is required.

### Warning messages
Warning messages are very flexible and can be described in very different ways:
```
// ;warn:$line+1:5: Warning with a placeholder $line (configurable with `linePlaceholder` option in save.toml)
```
```
// ;warn:35: Warning that points to the NEXT line of the code (no need to set line number explicily)
```
```
// ;warn:3:1: Warning with an explicit set of a line number and column number
```

### Regular expressions in warnings
Regular expressions can be used in warning messages.
To configure delimiters, use `patternForRegexInWarning` option (default: `"{{", "}}"`):
```
// ;warn:35: Warning [that] points{{ my regex.* }}to the NEXT line of the code{{.*}}
```
No need to escape special symbols outside of delimiters. It will be done automatically.

### Configuration
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
    // ;warn:2:14: Trailing semicolon {{.*is.*}} redundant in Kotlin
     val b: String;
     fun B(): String {}
     fun setB(): String {}
}
```

you will need the following SAVE configuration:

`save.toml`:
```toml
[general]
execCmd = "./detekt"
description = "My suite description"
suiteName = "DocsCheck"
# warning is set inside the comment in code, `//` marks comment start in Java
expectedWarningsPattern = "// ;warn:(\\d+):(\\d+): (.*)" # (default value)


[warn]
execFlags = "--build-upon-default-config -i"

# e.g. `WARN - 10/14 - Class name is in incorrect case`
# expected regex may allow an empty group for line number
# regex group with lineCaptureGroupIdx may include a number or linePlaceholder and addition/subtraction of a number
actualWarningsPattern = "\\w+ - (\\d+)/(\\d+) - (.*)$" # (default value)

# index of regex capture group for line number, used when `warningTextHasLine == true`
lineCaptureGroup = 2 # (default value)

# index of regex capture group for column number, used when `warningTextHasColumn == true`
columnCaptureGroup = 3 # (default value)

# index of regex capture group for message text
messageCaptureGroup = 4 # (default value)

warningTextHasColumn = true # (default value)
warningTextHasLine = true # (default value)
testNameRegex = ".*Test.*" # (default value)
batchSize = 1 # (default value)
batchSeparator  = ", " # (default value)
defaultLineMode = false
linePlaceholder = "$line"
patternForRegexInWarning = ["{{", "}}"]
# if true - the regex created from expected warning will be wrapped with '.*': .*warn.*.
partialWarnTextMatch = false # (default value)
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all files
matching `inputFilePattern`. It will then execute `$execCmd $testFile`. `batchSize` it controls how many files execCmd will process at a time. (since we specified
`batchSize = 1`, it will provide inputs one by one) and compare warnings its stdout (as per `output` option) parsed using `warningsOutputPattern` with warnings
parsed from the same `$testFile` using `warningsInputPattern`. `batchSeparator` is separator for filenames in `execCmd` if `batchSize > 1`.
If line number is not present in the comment, it's assumed to be `current line + 1` in regex group with lineCaptureGroupIdx. 
`linePlaceholder` is an optional placeholder for the line number that is recognized as the current line and supports addition and subtraction.

`warningsInputPattern` and `warningsOutputPattern` must include some mandatory capture groups: for line number (if `warningTextHasLine` is true),
for column number (if `warningTextHasColumn` is true) and for warning text. Their indices can be customized
with `lineCaptureGroup`, `columnCaptureGroup` and `messageCaptureGroup` parameters. These parameters are shared between input and output pattern;
usually you'll want them to be consistent to make testing easier, i.e. if input has line number, then so should output.
`testNameRegex` is a regular expression which sets the name of the test file.

### Customize `execCmd` per file with placefolders and execFlags
As the next level of customization, execution command can be customized per individual test. To do so, one can use a special comment in that file.
The pattern of the comment is taken from `WarnPluginConfig.runConfigPattern`. It should contain a single capture group, which corresponds to
execution command.

Additionally, that execution command can define a number of placeholders, which can be used in `execFlags` in TOML config:
* `args1` a set of CLI parameters which will be inserted _between_ `execFlags` from TOML config and name of the test file
* `args2` a set of CLI parameters which will be inserted _after_ the name of the test file
These placeholders are optional; if present, they should be comma-separated. Equal sign can be escaped with `\`. They can be accessed
from `warn.execFlags` with `$` sign. Additionally, `$fileName` in `execFlags` is substituted by the name of analyzed file
(or a set of names in batch mode).

For example, the comment `// RUN: args1=--foo\=bar,args2=--debug` in combination with `warn.execCmd = ./my-tool` will lead to execution
of the following command when checking file `FileName`:
```bash
./my-tool --foo=bar FileName --debug
```

The following images explain how `execFlags` can be used:

![image](https://user-images.githubusercontent.com/58667063/137911101-2fd15061-4d9a-4e54-a40e-0d136ff81e47.png)
![image](https://user-images.githubusercontent.com/58667063/137928360-0c3b8615-40c9-4fe3-8b4e-7c640b385491.png)



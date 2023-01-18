![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/saveourtool/save-cli)
![Maven Central](https://img.shields.io/maven-central/v/com.saveourtool.save/save-common)
![Lines of code](https://img.shields.io/tokei/lines/github.com/saveourtool/save-cli)
[![Hits-of-Code](https://hitsofcode.com/github/saveourtool/save-cli?branch=main)](https://hitsofcode.com/github/saveourtool/save-cli/view?branch=main)
![GitHub repo size](https://img.shields.io/github/repo-size/saveourtool/save-cli)
[![Run deteKT](https://github.com/saveourtool/save-cli/actions/workflows/detekt.yml/badge.svg)](https://github.com/saveourtool/save-cli/actions/workflows/detekt.yml)
[![Run diKTat](https://github.com/saveourtool/save-cli/actions/workflows/diktat.yml/badge.svg)](https://github.com/saveourtool/save-cli/actions/workflows/diktat.yml)
[![Build and test](https://github.com/saveourtool/save-cli/actions/workflows/build_and_test.yml/badge.svg)](https://github.com/saveourtool/save-cli/actions/workflows/build_and_test.yml)
[![License](https://img.shields.io/github/license/saveourtool/save-cli)](https://github.com/saveourtool/save-cli/blob/main/LICENSE)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cli.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cli?ref=badge_shield)
[![codecov](https://codecov.io/gh/saveourtool/save-api/branch/master/graph/badge.svg)](https://codecov.io/gh/saveourtool/save-api)

<!--
# ![codebeat badge] TO DO
# ![maintainability] TO DO
-->

Save is an all-purpose command-line test framework that could be used for testing of development tools,
especially which work with the code. Fully native and multiplatform application.  

## Quick start

|  |  |  |  |  |  |
| --- | --- | --- | --- | --- | --- |
|[CLI properties](/OptionsTable.md)|[examples](/examples/kotlin-diktat)|[save.toml config](#save_toml_configuration_file)|[Warn plugin](save-plugins/warn-plugin/README.md) | [Fix plugin](save-plugins/fix-plugin/README.md) | [Save presentation](/readme/save.pdf)|


## What is SAVE?
Static Analysis Verification and Evaluation (SAVE) - is an eco-system (see also [save-cloud](https://github.com/saveourtool/save-cloud)) for evaluation, testing and certification of static analyzers.
Instead of writing your own test framework, you can use SAVE to have a command-line test application. The only thing you need is to prepare test resources in a proper format.

<!-- 
SAVE supports 5 different categories of static analysis and can be used for testing tools that are used both for checking and for autofixing the code.
All of them have the description and special test packages:
- code style issues
- functional bugs
- security issues
- code smells
- best practices
-->
  
Save can be used not only with static analyzers, but can be used as a test framework for writing functional tests for other development tools, like compilers (as testing principles remain the same).

<img src="/readme/static-analysis-process.png" width="500px"/>

## How to start
1. Prepare and configure your test base in the proper format. See [test_detection](#test_detection) and [plugins](#plugins)
2. Run the following: `save "/my/path/to/tests"`. Directory `tests` should contain `save.toml` configuration file. 

## Plugins with examples
Here is a list of standard default plugins:
* [warn plugin](save-plugins/warn-plugin/README.md) for testing tools that find problems in the source code and emit warnings
* [fix plugin](save-plugins/fix-plugin/README.md) for testing tools for static analyzers that mutate text
* [fix-and-warn plugin](save-plugins/fix-and-warn-plugin/README.md) optimization in case you would like to fix file and after that check warnings that the tool was not able to fix in one execution.

In case you would like to have several plugins to work in your directory with same test files (resources), just simply add them all to `save.toml` config:
```text
[general]
...

[fix]
...

[warn]
...

[other plugin]
...
```

## Save warnings DSL 
![save-cli](https://user-images.githubusercontent.com/58667063/146390474-71e4921d-416b-4922-b2ea-894f71e491c3.jpg)
You can read more about the `warn plugin` [here](save-plugins/warn-plugin/README.md)


## How to configure 
SAVE has a command line interface that runs the framework and your executable. What you need is simply to configure the output of your static analyzer so SAVE will be able to
check if the proper error was raised on the proper line of test code.

To check that the warning is correct for SAVE - your static analyzer must print the result to stderr/stdout or to some log file.

General behavior of SAVE can be configured using command line arguments, or a configuration file `save.properties` that should be placed in the same folder with a root test config `save.toml`.

For the complete list of supported options that can be passed to SAVE via command line or save.properties file, please refer to the [options table](/OptionsTable.md) or run `save --help`.
Note, that options with choice are case-sensitive.

Example of `save.properties` file:
```properties
reportType=plain
language=c++
```

OR you can pass these arguments directly in command line: 
```bash
save --report-type json --language java
```

SAVE framework is able to automatically detect your tests, run your analyzer on these tests, calculate the pass-rate and return test results in the expected format.

## <a name="test_detection"></a> Test detection and save.toml file
To make SAVE detect your test suites you need to put `save.toml` file in each directory where you have tests that should be run.
Note, that these configuration files inherit configurations from the previous level of directories.

Despite the fact, that almost all fields may not be defined in bottom levels and can be inherited from the top level,
you should be accurate: some fields in `[general]` section are required for execution, so you need to provide them at least in one config from inheritance chain
for test that should be run.
[Look which fields are required](#save_toml_configuration_file).

For example, in case of the following hierarchy of directories:
```text
| A
  | save.toml
  | B
    | save.toml
```

`save.toml` from the directory B will inherit settings and properties from directory A.

Please note, that SAVE will detect all files with Test postfix and will automatically use configuration from `save.toml` file that is placed
in the directory. Tests are named by the test file resource name without a suffix 'Test'.
In case SAVE will detect a file with Test postfix in test resources and will not be able to find any `save.toml` configurations
in the hierarchy of directories - it will raise an error.

For example, the following example is invalid and will cause an error, because SAVE framework will not be able to find `save.toml` configuration file:
```text
| A
  | B
  | myTest.java
```

As described above, `save.toml` is needed to configure tests. The idea is to have only one configuration file for a directory with tests (one to many relation). 
Such directories we will call `test suites`. We decided to have only one configuration file as we have many times seen that for such tests there is a duplication of configuration in the same test suite.

## <a name="save_toml_configuration_file"></a> save.toml configuration file
Save configuration uses [toml](https://toml.io/en/) format. As it was told [above](#test_detection), save.toml can be imported from the directory hierarchy.
The configuration file has `[general]` table and `[plugins]` table. To see more information about plugins, read [this](#plugins) section.
In this section we will give information only about the `[general]` table that can be used in all plugins.

```text
[general]
# your custom tags that will be used to detect groups of tests (required)
tags = ["parsing", "null-pointer", e.t.c]

# custom free text that describes the test suite (required)
description = "My suite description"

# Simple suite name (required)
suiteName = DocsCheck, CaseCheck, NpeTests, e.t.c 

// FixMe: add tests that check that it is required and that it can be overwritten by child configs
# Execution command (required at least once in the configuration hierarchy)
execCmd="./ktlint -R diktat-0.4.2.jar"

# excluded tests in the suite (optional). Here you can provide names of excluded tests, separated by comma. By the default no tests are excluded. 
# to exclude tests use relative path to the root of test project (to the root directory of `save.toml`)
excludedTests = ["warn/chapter1/GarbageTest.kt", "warn/otherDir/NewTest.kt"], e.t.c

# command execution time for one test (milliseconds)
timeOutMillis = 10000

# language for tests
language = "Kotlin"
```

## Executing specific tests
It can be useful to execute only a number of tests instead of all tests under a particular `save.toml` config.
To do so, you want to pass a relative path to test file after all configuration options:
```bash
$ save [options] /path/to/tests/Test1
```
or a list of relative paths to test files (separated with spaces)
```bash
$ save [options] /path/to/tests/Test1 /path/to/tests/Test2
```
SAVE will detect the closest `save.toml` file and use configuration from there.

`Note:` On Windows, you may need to use double backslash `\\` as path separator


## <a name="plugins"></a> Using plugins for specific test-scenarios
SAVE doesn't have any inspections active by default, instead the behavior of the analysis is fully configurable using plugins.

// FixMe: Custom plugins are not yet fully supported. Do not use custom pluins.
Plugins are dynamic libraries (`.so` or `.dll`) and they should be provided using argument `--plugins-path`. Some plugins are bundled
with SAVE out-of-the-box and don't require an additional setup. 

## SAVE output
Save supports several formats of test result output: `PLAIN` (markdown-like table with all test results), `PLAIN_FAILED`
(same as `PLAIN`, but doesn't show passed tests) and `JSON` (structured representation of execution result).
The format could be selected with `--report-type` option.

## Purpose of Static Analysis Verification and Evaluation (SAVE) project
Usage of [static analyzers](https://en.wikipedia.org/wiki/Static_program_analysis) - is a very important part of development each and every software product.
All human beings can make a mistake in their code even when a software developer is writing all kinds of tests and has a very good test-coverage.
All these issues can lead to potential money losses of companies. Static analysis of programs helps to reduce the number of such bugs and issues
that cannot be found by validations on the compiler's side.

There are different kinds and purposes of static analysis: it can be simple analysis using AST (abstract syntax tree), it can be more complex CFA
(control-flow analysis), interprocedural analysis, context sensitive analysis, e.t.c. Static analyzers can check code style, find potential issues on the runtime in
the logic of an application, check for code smells and suggest best practices. But what exactly should static analyzers do? How their functionality can be measured?
What is an acceptance criteria for Which functionality do developers really need when they are writing a brand new analyzer? These questions are still remain not answered,
even after decades of development of static analyzers.

## Problematics
Each and every creator of static analyzers in the beginning of his development journey starts
from the very simple thing: types of issues that his tool will detect. This leads to a searching of existing lists of potential issues or test packages that can be used to
measure the result of his work or can be used for TDD (test driven development). In other areas of system programming such benchmarks and test sets already exists,
for example [SPEC.org](http://spec.org/benchmarks.html) benchmarks are used all over the world to test the functionality, evaluate and measure the performance of different applications
and hardware: from compilers to CPUs, from web-servers to Java Clients. But there are no test sets and even strict standards for detection of issues that can be found in
popular programming languages. There were some guidelines of coding on C/C++ done by [MISRA](https://www.misra.org.uk/), but there are no analogues of it even for the most popular
languages in the world like Python and [JVM-languages](https://stackoverflow.com/questions/6050618/is-there-a-java-equivalent-to-misra-c). There are only existing test suites at [NIST](https://samate.nist.gov/SRD/testsuite.php), but the framework and eco-system remain limited.

In this situation each and every new developer that reinvents his new code style or mechanism of static analysis each time reinvents his brand new test framework and writting test sets
that have been written already thousands of times for his analyzer/linter. Someone uses existing guidelines like [Google code style](https://google.github.io/styleguide/javaguide.html)
or using [PMD rules](https://pmd.github.io/). But in all cases a lot of time will be spent on reinventing, writing and debuging tests.

## Development
### Build
The project uses gradle as a build system and can be built with the command `./gradlew build`.
To compile native artifacts, you will need to install prerequisites as described in Kotlin/Native documentation.

To access dependencies hosted on Github Package Registry, you need to add the foolowing into `gradle.properties` or `~/.gradle/gradle.properties`:
```properties
gprUser=<GH username>
gprKey=<GH personal access token>
```
Personal Access Token should be generated via https://github.com/settings/tokens/new with the scope at least containing `read:packages`.

Because of generated code, you will need to run the build once to correctly import project in IDE with resolved imports.

## Contribution
You can always contribute to the main SAVE framework - just create a PR for it. But to contribute or change tests in categories you will need get approvals from 
the maintainer of the test package/analysis category. Please see the list of them.  


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cli.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cli?ref=badge_large)
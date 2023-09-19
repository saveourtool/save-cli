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

Save is an all-purpose command-line test framework that can be used for testing tools that work with code, 
such as static analyzers and compilers. It is a fully native application, requiring no need to install any SDK.

## What is SAVE?
Static Analysis Verification and Evaluation (SAVE) is an ecosystem (also see [save-cloud](https://github.com/saveourtool/save-cloud)) designed for the evaluation, 
testing, and certification of static analyzers, compilers or any other software tools. Instead of developing your own test 
framework, you can utilize SAVE as a command-line test application. The only requirement is to prepare test 
resources in the appropriate format.

## Contribution
We need your help! We will be glad if you will use, test or contribute to this project.
In case you don't have much time for this - at least **give us a star** to attract other contributors! 
Thanks! 🙏 🥳

## Quick start: User scenarios
### 1. Static analysis, warnings, sequentially
> - My code analysis tool processes files **sequentially, one by one**;
> - It produces **warnings** and outputs them to **stdout**;
> - I want to compare actual warnings with expected warnings that are specified **within the test resource code**.

### 2. Static analysis, warnings, processing whole project
> - I also have code analysis tool, but it processes **the entire project** at once and is aware of all the code **relations**.;
> - It produces **warnings** and outputs them to **stdout**;
> - I want to compare actual warnings with expected warnings that are specified **within the test resource code**.

### 3. Automated code fixing or generation
> - My tool **manipulates** the original code, for example, by auto-fixing it;
> - I would like to check how my tool **fixes the code** by comparing it with expected result;
> - Additionally, it can be used by compilers to validate **code generation**, **transitioning** from the original source 
> - code to **intermediate representation** (IR), another programming language, or even assembly.

### 4. Expected warnings in a separated file
> - I do not want to specify my expected warnings in code; 
> - I prefer to use **a separate file** in SARIF or any other format.

## How to Run
1. Download [the latest release](https://github.com/saveourtool/save-cli) suitable for your OS and architecture.
2. Set up and configure your test base in the correct SAVE format. Refer to [test_detection](#test_detection) and [plugins](#plugins) for guidance.
3. Execute the following command (modify it according to your architecture and OS): `save "/my/path/to/tests"`

Ensure the `tests` directory contains the `save.toml` configuration file.

## SAVE Logging

To debug SAVE execution, you can use the following argument:
`--log=TYPE`, where `TYPE` can be one of the following:

- `all` - Comprehensive logging that includes all information from SAVE execution, even more detailed than DEBUG (akin to a trace).
- `debug` - Displays results, warnings, and debug information.
- `warnings` - Shows results and critical warnings.
- `results_only` - Displays only the results.

## Plugins with examples

<img src="/readme/static-analysis-process.png" width="500px"/>

Here is a list of standard plugins:
* [warn plugin](save-plugins/warn-plugin/README.md): This is for testing tools that detect issues in the source code and produce warnings.
* [fix plugin](save-plugins/fix-plugin/README.md): This is used for testing static analyzer tools that modify text.
* [fix-and-warn plugin](save-plugins/fix-and-warn-plugin/README.md): An optimization for scenarios where you want to correct a file and subsequently check for warnings that the tool couldn't address in a single run.

If you want multiple plugins to operate in your directory using the same test files (resources), simply add them all to the `save.toml` configuration:

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

## How to Configure

SAVE has a command-line interface that allows you to run both the framework and your executable. Your main task is to configure the output of your static analyzer so that SAVE can verify whether the appropriate error was flagged at the correct line of the test code.

To ensure the warning is accurate for SAVE, your static analyzer must output the result either to stderr/stdout or a designated log file (for example in Sarif format).

You can configure SAVE's general behavior using command-line arguments or by using a configuration file named `save.properties`. This file should be located in the same directory as the root test config, `save.toml`.

For a comprehensive list of options that can be passed to SAVE via the command line or the `save.properties` file, refer to the [options table](/OptionsTable.md) or execute the `save --help` command. Please be aware that options with choices are case-sensitive.

The SAVE framework will automatically **detect** your tests, run your analyzer on them, calculate the pass rate, and return test results in the expected format.

## <a name="test_detection"></a> Test Detection and save.toml File
To enable SAVE to detect your test suites, you must place a `save.toml` file in each directory containing **test suites**. It's important to note that these configuration files inherit configurations from parent directories.

Although most fields can be left undefined at lower levels and can inherit values from top levels, you should be cautious. 
Some fields in the `[general]` section are mandatory for execution, so you need to specify them in at least one config file in the inheritance chain for tests that are meant to run. 
[Check which fields are mandatory](#save_toml_configuration_file).

For instance, with the following directory hierarchy:
```text
| A
  | save.toml
  | B
    | save.toml
```
The `save.toml` in directory B will inherit settings and properties from directory A.

Bear in mind that SAVE will detect all files with the 'Test' postfix and will automatically utilize configurations from the `save.toml` file present in the same directory (or inherited from parent). 
Tests are named according to the test file's resource name, excluding the 'Test' suffix. 
If SAVE detects a file with the 'Test' postfix in the test resources and cannot locate any `save.toml` configurations in the **directory hierarchy**, it will throw an error.

For instance, the scenario below is invalid and will trigger an error, as the SAVE framework cannot locate the `save.toml` configuration file:
```text
| A
  | B
  | myTest.java
```

As previously mentioned, the `save.toml` is essential for configuring tests. 
_Ideally_, there should be one configuration file for each directory containing tests, establishing a one-to-many relationship. 
We refer to these directories as `test suites`. 

The rationale behind having a single configuration file for one test suite is to avoid redundant configurations within the same test suite.

## <a name="save_toml_configuration_file"></a> save.toml Configuration File

The save configuration uses the [TOML](https://toml.io/en/) format powered by [ktoml](https://github.com/akuleshov7/ktoml) project.
As mentioned [above](#test_detection), `save.toml` can be inherited from the directory hierarchy (parent directories).

The configuration file contains a `[general]` table and a `[plugins]` table. For more information about plugins, refer to the [plugins section](#plugins).

In this section, we will provide information only about the `[general]` table, which can be used across all plugins.

```text
[general]
# Your custom tags that will be used to detect groups of tests (required)
tags = ["parsing", "null-pointer", "etc"]

# Custom free text that describes the test suite (required)
description = "My suite description"

# Simple suite name (required)
suiteName = "DocsCheck", "CaseCheck", "NpeTests", "etc" 

# Execution command (required at least once in the configuration hierarchy)
# By the default these binaries should be in the same directory of where SAVE is run 
# or should have full or relational path (root - is the directory with save executable)
execCmd="./ktlint -R diktat-0.4.2.jar"

# Excluded tests in the suite (optional). Here, you can list the names of excluded tests, separated by commas. By default, no tests are excluded.
# To exclude tests, use the relative path to the root of the test project (to the root directory of `save.toml`)
excludedTests = ["warn/chapter1/GarbageTest.kt", "warn/otherDir/NewTest.kt", "etc"]

# Command execution time for one test (in milliseconds)
timeOutMillis = 10000

# Language for tests
language = "Kotlin"
```

## Executing Specific Tests

At times, you might want to execute only a specific set of tests instead of running all the tests under a particular `save.toml` config. 
To achieve this, pass the relative path to the test file after all configuration options (root - is directory with save binary):

```bash
$ save [options] /path/to/tests/Test1
```

You can also provide a list of relative paths to test files (separated by spaces):

```bash
$ save [options] /path/to/tests/Test1 /path/to/tests/Test2
```

SAVE will automatically detect the nearest `save.toml` file and use the configuration from it.

`Note:` On Windows, remember to use a double backslash `\\` as the path separator.

## SAVE Output
SAVE supports several formats for test report output:
- `PLAIN`: A markdown-like table showing all test results.
- `PLAIN_FAILED`: Similar to `PLAIN`, but only displays failed tests.
- `JSON`: Structured representation of the execution result.

The desired format can be selected using the `--report-type=PLAIN` option.

## Purpose of Static Analysis Verification and Evaluation (SAVE) project
## Purpose of Static Analysis Verification and Evaluation (SAVE) Project

<details>
<summary>Purpose of SAVE</summary>

### Intro

The use of [static analyzers](https://en.wikipedia.org/wiki/Static_program_analysis) is an integral part of the development process for every 
software product. While software developers may write various tests and achieve good test coverage, human error remains inevitable. 
Such errors can result in significant financial losses for companies. Static program analysis assists in identifying and rectifying bugs 
and issues that might not be detectable through compiler validations alone.

Static analysis comes in various forms and serves different purposes. It might involve a simple analysis using an AST 
(abstract syntax tree) or delve into more complex procedures like CFA (control-flow analysis), interprocedural analysis, 
or context-sensitive analysis. Static analyzers can assess code style, pinpoint potential runtime issues in application logic, 
detect code smells, and suggest best practices. However, there remains a lack of clarity about the core functions of static analyzers. 
How can their efficacy be quantified? What criteria determine their acceptance? What functionalities are essential for developers creating 
a new analyzer? Despite years of static analyzer development, these questions remain largely unanswered.

### Problematics

At the onset of their development journey, every creator of a static analyzer begins with identifying the kinds of issues 
their tool will target. This often necessitates a search for existing lists of potential issues or test packages that can 
guide the development process, particularly if following a TDD (test-driven development) approach. While other domains in 
system programming have established benchmarks and test sets, such as the [SPEC.org](http://spec.org/benchmarks.html) benchmarks 
used globally to evaluate various software and hardware components, no such standards exist for identifying issues in popular 
programming languages. While guidelines for coding in C/C++ have been established by [MISRA](https://www.misra.org.uk/), 
there are no equivalents for widely used languages like Python and
[JVM-languages](https://stackoverflow.com/questions/6050618/is-there-a-java-equivalent-to-misra-c). 
There are test suites available at [NIST](https://samate.nist.gov/SRD/testsuite.php), but their framework and ecosystem are somewhat restrictive.

Given this scenario, developers often find themselves recreating mechanisms for static analysis or developing new test frameworks,
leading to repetitive work. Some might opt for existing guidelines such as the [Google code style](https://google.github.io/styleguide/javaguide.html) 
or [PMD rules](https://pmd.github.io/), but regardless of the approach, significant time is invariably spent on conceptualizing, writing, 
and debugging tests.

</details>

## Development
### Build
The project uses Gradle as its build system and can be built using the command `./gradlew build`.

To compile native artifacts, you must install the prerequisites as described in the Kotlin/Native documentation.

To access dependencies hosted on the GitHub Package Registry, add the following to either `gradle.properties` or `~/.gradle/gradle.properties`:
```properties
gprUser=<GH username>
gprKey=<GH personal access token>
```
A Personal Access Token can be generated at https://github.com/settings/tokens/new. Ensure the token has a scope that includes `read:packages`.

Due to the generated code, you need to **run the build once** to correctly import the project into an IDE with resolved imports.

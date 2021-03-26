![Build and test](https://github.com/cqfn/save/workflows/Build%20and%20test/badge.svg)
[![License](https://img.shields.io/github/license/cqfn/save)](https://github.com/cqfn/save/blob/master/LICENSE)
[![codecov](https://codecov.io/gh/cqfn/save/branch/master/graph/badge.svg)](https://codecov.io/gh/cqfn/save)

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
languages in the world like Python and [Java](https://stackoverflow.com/questions/6050618/is-there-a-java-equivalent-to-misra-c).
 
In this situation each and every new developer that reinvents his new code style or mechanism of static analysis each time reinvents his brand new test framework and writting test sets
that have been written already thousands of times for his analyzer/linter. Someone uses existing guidelines like [Google code style](https://google.github.io/styleguide/javaguide.html)
or using [PMD rules](https://pmd.github.io/). But in all cases lot of time will be spent on reinventing, writing and debuging tests.

## What is SAVE?
SAVE - is an eco-system for measuring, testing and certification of static code analyzers. Instead of writing your test framework SAVE will provide you a command line application with a
test sets for the language that you are developing analyzer for. It provides you also a service that can be used to determine the readiness of your tool. SAVE has a committee of static analysis experts
that regularly updates tests and discuss the best practices for particular programming languages.

SAVE supports 5 different categories of static analysis. All of them have the description and special test packages:
- code style 
- functional bugs
- security issues
- code smells
- best practices 

## SAVE CLI: how to configure 
SAVE has a command line interface that runs the framework and your executable. What you need is simply to configure the output of your static analyzer so SAVE will be able to
check if the proper error was raised on the proper line of test code. To check that the warning is correct your static analyzer must print the result to stderr/stdout or to some log file.
 
 
If you don't want to make any configuration - you can simply create an adaptor from your style of warnings/errors to the **default** style of warnings in SAVE framework:
```bash
WARN FileName.your_extension:line:column: your warning text 
``` 

Configuration file that can be provided to save (`save -prop $PATH_TO_PROPS/save.properties`), `save.properties`:
```bash
exec_cmd = "my_analyzer -my_option1 -my_option2"
result_output = stderr/stdout/file
suppressed_tests = "list of tests that should be suppressed, separated by comma"
mode = single/parallel
language = java/cpp/c/python/kotlin
category = security/style/smells/bugs/practices
# to configure the format of warnings that will be raised by your analyzer
warn_format = ".*$filename.format:$line:$column:.*" 
```

OR you can pass these arguments directly in command line:
save -exec_cmd="my_analyzer -my_option1 -my_option2"

SAVE framework will run your analyzer on the following test packages and calculate the pass-rate. 

## SAVE: Using plugins for specific inspections
SAVE doesn't have any inspections active by default, instead the behavior of the analysis is fully configurable using plugins.
Plugins are dynamic libraries (`.so` or `.dll`) and they should be provided using argument `--plugins-path`. Some plugins are bundled
with SAVE out-of-the-box and don't require additional setup. Here is a list of standard plugins:
* [diff-plugin](save-plugins/diff-plugin/README.md) for testing tools that mutate text
* [warnings-plugin](save-plugins/warnings-plugin/README.md) for testing tools that find problems in code and emit warnings

Extending SAVE and writing your own plugin is simple. For instructions, see [corresponding README](save-plugins/README.md).

## SAVE: writing your test packages and running them with SAVE:
With option `-suite` your can provide to SAVE a path to your custom test suites. 
Please note, that SAVE has special notation of tests:
```java
// warn:1:6: Class name should be in an uppercase format
class a {
     String b;
     String getB() {}
     String setB() {}
}
```

## SAVE dashboard: how to use
SAVE provides the dashboard that can be run on your server and output the pass-rate (in percents) and existing test failures for your static analyzer.
To start you will need to install java on your server and run:
```bash
java -jar save-board.jar
``` 

## Contribution
You can always contribute to the main SAVE framework or to a dashboard - just create a PR for it. But to contribute or change tests in categories you will need get approvals from 
the maintaner of the test package/analysis category. Please see the list of them.  


====== Test framework
# sactest
Test framework for Static Analyzers and Compilers

# options 
1. help
2. workers (threads)
3. config 
4. debug
5. directory to detect and run all tests inside
5. quiet (just report?)
6. report type (xml, json, e.t.c)
7. exclude tests by name
8. list of tests run by name


# config file
TOML format
```toml
[test]
name = SomeTestName
source = /usr/test/Test.java (optional, first should try to resolve by TestName)
expected = /usr/test/Test.java (optional, can be missing or will be resolved by TestName)
tags = parsing, null-pointer, e.t.c
description = (optional??)

[compiler]
testType = frontend, backend, codegen, translator
binaryPath = /usr/bin/

[static analyzer]
testType = checker, fixer
binaryPath = /usr/bin/
```

# can be used as functional tests and as functional tests


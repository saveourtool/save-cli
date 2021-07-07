## Save fix-and-warn plugin
Plugin that runs provided executable on the initial file with a test source code
and combines two actions during one execution: 
[fix](save-plugins/fix-plugin/README.md) and [warn](save-plugins/warn-plugin/README.md).\
Plugin fix test file, warns if something couldn't be auto-corrected after fix
and compares output with expected output during one execution.

There are no additional settings, which should be provided for this plugin,
it just should contain subsections `[fix]` and `[warn]`, which should be configured,
like it described in corresponding README files.

Please note, that it is only important for test resources to have the same postfixes in 
`[fix]` and `[warn]` sections.
By the default test file it is `Test`,
for the file with expected result - it is `Expected`.


## Configuration
Follow the instructions for `[fix]` and `[warn]` plugins and just add
their configuration as a subsections of `[fix-and-warn]` plugin.
```toml
[general]
execCmd="./ktlint -R diktat-0.4.2.jar"
description = "My suite description"
suiteName = "DocsCheck"

[fix-and-warn]
    [fix]
        execFlags="-F"
        testFilePattern="*Test.kt"
        expectedFilePattern="*Expected.kt"
        batchSize = 1
        batchSeparator = ", "
        resourceNameTestSuffix = "Test"
        resourceNameExpectedSuffix = "Expected"
    [warn]
        execFlags = "--build-upon-default-config -i"
        warningsInputPattern = "// ;warn:(\\d+):(\\d+): (.*)"
        warningsOutputPattern = "\\w+ - (\\d+)/(\\d+) - (.*)$"
        testNameSuffix = "Test"
```
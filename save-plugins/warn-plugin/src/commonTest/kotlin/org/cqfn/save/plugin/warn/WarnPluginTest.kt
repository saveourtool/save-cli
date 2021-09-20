package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.ExtraFlags
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.ResourceFormatException
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WarnPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPluginTest::class.simpleName!!)
    private val catCmd = if (isCurrentOsWindows()) "type" else "cat"
    private val mockScriptFile = tmpDir / "resource"
    private val defaultGeneralConfig = GeneralConfig(
        execCmd = "",
        tags = listOf(""),
        description = "",
        suiteName = "",
        expectedWarningsPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)"),
        runConfigPattern = GeneralConfig.defaultRunConfigPattern,
    )
    private val defaultWarnConfig = WarnPluginConfig(
        execFlags = "$catCmd $mockScriptFile && set stub=",
        warningTextHasLine = true,
        warningTextHasColumn = true,
        batchSize = 1,
        batchSeparator = ", ",
        lineCaptureGroup = 1,
        columnCaptureGroup = 2,
        messageCaptureGroup = 3,
        fileNameCaptureGroupOut = 1,
        lineCaptureGroupOut = 2,
        columnCaptureGroupOut = 3,
        messageCaptureGroupOut = 4
    )

    private fun mockExecCmd(stdout: String) = fs.write(fs.createFile(mockScriptFile)) {
        write(stdout.encodeToByteArray())
    }

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    @Test
    fun `basic warn-plugin test`() {
        mockExecCmd(
            """
                |Test1Test.java:4:6: Class name should be in PascalCase
            """.trimMargin()
        )
        performTest(
            listOf(
                """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent()
            ),
            defaultWarnConfig,
            defaultGeneralConfig
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test with default warning without line`() {
        mockExecCmd(
            """
                |Test1Test.java:5: Class name should be in PascalCase
                |Test1Test.java:5: Class name shouldn't have a number
                |Test1Test.java:7: Variable name should be in LowerCase
                |Test1Test.java:10: Class should have a Kdoc
                |Test1Test.java:10: Class name should be in PascalCase
                """.trimMargin()
        )
        performTest(
            listOf(
                """
                package org.cqfn.save.example
                
                // ;warn: Class name should be in PascalCase
                // ;warn: Class name shouldn't have a number
                class example1 {
                // ;warn: Variable name should be in LowerCase
                    int Foo = 42;
                }
                // ;warn: Class should have a Kdoc
                // ;warn:10: Class name should be in PascalCase
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                actualWarningsPattern = Regex("(.+):(\\d+): (.+)"),
                warningTextHasColumn = false,
                lineCaptureGroup = 1,
                columnCaptureGroup = null,
                messageCaptureGroup = 2,
                fileNameCaptureGroupOut = 1,
                lineCaptureGroupOut = 2,
                columnCaptureGroupOut = null,
                messageCaptureGroupOut = 3,
            ),
            defaultGeneralConfig.copy(expectedWarningsPattern = Regex("// ;warn:?(\\d*): (.*)"))
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass, "Expected result to be a single pass, but got $results")
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test for all mods`() {
        mockExecCmd(
            """
                |Test1Test.java:1:1: Package name is incorrect
                |Test1Test.java:6:1: Class name should be in PascalCase too
                |Test1Test.java:6:1: Class name should be in PascalCase
                |Test1Test.java:6:1: Class name shouldn't have a number
                |Test1Test.java:9:1: Variable name should be in LowerCase
                """.trimMargin()
        )
        performTest(
            listOf(
                """
                // ;warn:1:1: Package name is incorrect
                package org.cqfn.save.example
                
                // ;warn:1: Class name should be in PascalCase too
                // ;warn:${'$'}l+1:1: Class name shouldn't have a number
                class example1 {
                // ;warn:${'$'}l-1:1: Class name should be in PascalCase
                // ;warn:${'$'}l+1:1: Variable name should be in LowerCase
                    int Foo = 42;
                }
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                actualWarningsPattern = Regex("(.+):(\\d+):(\\d*): (.*)"),
                linePlaceholder = "\$l",
            ),
            defaultGeneralConfig.copy(expectedWarningsPattern = Regex("// ;warn:?(.*):(\\d+): (.*)"))
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass, "Expected result to be a single pass, but got $results")
        }
    }

    @Test
    fun `basic warn-plugin test with exactWarningsMatch = false`() {
        mockExecCmd(
            """
                |Test1Test.java:4:6: Class name should be in PascalCase
                |Test1Test.java:5:8: Variable name should be in lowerCamelCase
                """.trimMargin()
        )
        performTest(
            listOf(
                """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int Foo = 42;
                }
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                exactWarningsMatch = false,
            ),
            defaultGeneralConfig
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
            val nameWarn =
                    "Some warnings were unexpected: [Warning(message=Variable name should be in lowerCamelCase, line=5, column=8, fileName=Test1Test.java)]"
            assertEquals(nameWarn, (results.single().status as Pass).message)
        }
    }

    @Test
    @Ignore  // this logic is todo
    fun `basic warn-plugin test with ignoreTechnicalComments=true`() {
        performTest(
            listOf(
                """
                // ;warn:1:1: Avoid using default package
                // ;warn:3:6: Class name should be in PascalCase
                class example {
                    // ;warn:5:5: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn:7:1: File should end with trailing newline
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                execFlags = "echo Test1Test.java:4:6: Class name should be in PascalCase",
            ),
            defaultGeneralConfig
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test - multiple warnings`() {
        mockExecCmd(
            """Test1Test.java:1:1: Avoid using default package
                    |Test1Test.java:3:6: Class name should be in PascalCase
                    |Test1Test.java:5:5: Variable name should be in lowerCamelCase
                    |Test1Test.java:7:1: File should end with trailing newline
                    |""".trimMargin()
        )
        performTest(
            listOf(
                """
                // ;warn:1:1: Avoid using default package
                // ;warn:3:6: Class name should be in PascalCase
                class example {
                    // ;warn:5:5: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn:7:1: File should end with trailing newline
            """.trimIndent()
            ),
            defaultWarnConfig,
            defaultGeneralConfig
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Ignore  // this logic is todo
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test - multiple warnings & ignore technical comments`() {
        mockExecCmd(
            """Test1Test.java:1:1: Avoid using default package
                    |Test1Test.java:3:6: Class name should be in PascalCase
                    |Test1Test.java:5:5: Variable name should be in lowerCamelCase
                    |Test1Test.java:7:1: File should end with trailing newline
                    |""".trimMargin()
        )
        performTest(
            listOf(
                """
                // ;warn:1:1: Avoid using default package
                // ;warn:1:6: Class name should be in PascalCase
                class example {
                    // ;warn:2:5: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn:3:1: File should end with trailing newline
            """.trimIndent()
            ),
            defaultWarnConfig,
            defaultGeneralConfig.copy(expectedWarningsPattern = Regex("(.+):(\\d+):(\\d+): (.+)")),
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test - multiple warnings, no line-col`() {
        mockExecCmd(
            """Test1Test.java: Avoid using default package
                    |Test1Test.java: Class name should be in PascalCase
                    |Test1Test.java: Variable name should be in lowerCamelCase
                    |Test1Test.java: File should end with trailing newline
                    |""".trimMargin()
        )
        performTest(
            listOf(
                """
                // ;warn: Avoid using default package
                // ;warn: Class name should be in PascalCase
                class example {
                    // ;warn: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn: File should end with trailing newline
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                actualWarningsPattern = Regex("(.+): (.+)"),
                warningTextHasLine = false,
                warningTextHasColumn = false,
                lineCaptureGroup = null,
                columnCaptureGroup = null,
                messageCaptureGroup = 1,
                fileNameCaptureGroupOut = 1,
                lineCaptureGroupOut = null,
                columnCaptureGroupOut = null,
                messageCaptureGroupOut = 2,
            ),
            defaultGeneralConfig.copy(expectedWarningsPattern = Regex("// ;warn: (.*)")),
        ) { results ->
            assertEquals(1, results.size)
            results.single().status.let {
                assertTrue(it is Pass, "Expected test to pass, but actually got status $it")
            }
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test for batchSize`() {
        mockExecCmd(
            """
                |Test1Test.java:4:6: Class name should be in PascalCase
                |Test2Test.java:2:3: Class name should be in PascalCase
                """.trimMargin()
        )
        performTest(
            listOf(
                """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent(),
                """
                package org.cqfn.save.example
                
                // ;warn:2:3: Class name should be in PascalCase
                class example2 {
                    int foo = 42;
                }
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                batchSize = 2
            ),
            defaultGeneralConfig
        ) { results ->
            assertEquals(2, results.size)
            assertTrue(results.all { it.status is Pass })
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `regression - test resources in multiple directories`() {
        mockExecCmd(
            """
                |
                """.trimMargin()
        )
        fs.createFile(tmpDir / "Test1Test.java")
        fs.createFile(tmpDir / "Test2Test.java")
        fs.createDirectory(tmpDir / "inner")
        fs.createFile(tmpDir / "inner" / "Test3Test.java")
        fs.createFile(tmpDir / "inner" / "Test4Test.java")
        performTest(
            emptyList(),  // files will be discovered in tmpDir, because they are already created
            defaultWarnConfig.copy(
                batchSize = 2,
            ),
            defaultGeneralConfig
        ) { results ->
            assertEquals(4, results.size)
            assertTrue(results.all { it.status is Pass })
        }
    }

    @Test
    fun `warn-plugin test exception`() {
        assertFailsWith<ResourceFormatException> {
            "// ;warn:4:6: Class name should be in PascalCase".extractWarning(
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                "fileName",
                1,
                5,
                2,
            )
        }
    }

    @Test
    fun `should resolve placeholders`() {
        // basic test
        checkPlaceholders(
            "--foo --bar testFile --baz",
            "--foo \$args1 \$fileName \$args2",
            ExtraFlags("--bar", "--baz"),
            "testFile"
        )
        // only beforeFlags
        checkPlaceholders(
            "--foo --bar testFile",
            "--foo \$args1 \$fileName",
            ExtraFlags("--bar", ""),
            "testFile"
        )
        // only afterFlags
        checkPlaceholders(
            "--foo testFile --baz",
            "--foo \$fileName \$args2",
            ExtraFlags("", "--baz"),
            "testFile"
        )
        // only fileName
        checkPlaceholders(
            "--foo testFile",
            "--foo \$fileName",
            ExtraFlags("", ""),
            "testFile"
        )
        // no flags
        checkPlaceholders(
            "--foo testFile",
            "--foo",
            ExtraFlags("", ""),
            "testFile"
        )
    }

    private fun checkPlaceholders(
        execFlagsExpected: String,
        execFlagsFromConfig: String,
        extraFlags: ExtraFlags,
        fileName: String,
    ) {
        assertEquals(
            execFlagsExpected,
            WarnPlugin(
                TestConfig(fs.createFile(tmpDir / "save.toml"), null, mutableListOf(defaultWarnConfig, defaultGeneralConfig), fs),
                testFiles = emptyList(),
                fs
            )
                .resolvePlaceholdersFrom(execFlagsFromConfig, extraFlags, fileName)
        )
    }

    private fun performTest(
        texts: List<String>,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
        assertion: (List<TestResult>) -> Unit
    ) {
        val config = fs.createFile(tmpDir / "save.toml")
        texts.forEachIndexed { idx, text ->
            val testFileName = "Test${idx + 1}Test.java"
            fs.write(fs.createFile(tmpDir / testFileName)) {
                write(text.encodeToByteArray())
            }
        }

        val results = WarnPlugin(
            TestConfig(config, null, mutableListOf(warnPluginConfig, generalConfig), fs),
            testFiles = emptyList(),
            fs
        )
            .execute()
            .toList()
        println(results)
        assertion(results)
    }
}

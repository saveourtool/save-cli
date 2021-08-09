package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.ResourceFormatException
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem
import okio.Path.Companion.toPath

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
    private val defaultWarnConfig = WarnPluginConfig(
        "$catCmd $mockScriptFile && set stub=",
        Regex("// ;warn:(\\d+):(\\d+): (.*)"),
        true, true, 1, ", ",
        1, 2, 3, 1, 2, 3, 4
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
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test for defaultLineMode`() {
        mockExecCmd(
                """
                |Test1Test.java:5: Class name should be in PascalCase
                |Test1Test.java:5: Class name shouldn't have a number
                |Test1Test.java:7: Variable name should be in LowerCase
                |Test1Test.java:10: Class should have a Kdoc
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
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                actualWarningsPattern = Regex("// ;warn: (.*)"),
                warningTextHasColumn = false,
                lineCaptureGroup = null,
                columnCaptureGroup = null,
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test for placeholder`() {
        mockExecCmd(
                """
                |Test1Test.java:4:1: Class name should be in PascalCase
                |Test1Test.java:4:1: Class name shouldn't have a number
                |Test1Test.java:7:1: Variable name should be in LowerCase
                """.trimMargin()
            )
        performTest(
            listOf(
                """
                package org.cqfn.save.example
                
                // ;warn:${'$'}l+1:1: Class name shouldn't have a number
                class example1 {
                // ;warn:${'$'}l-1:1: Class name should be in PascalCase
                // ;warn:${'$'}l+1:1: Variable name should be in LowerCase
                    int Foo = 42;
                }
            """.trimIndent()
            ),
            defaultWarnConfig.copy(
                defaultLineMode = false,
                linePlaceholder = "\$l",
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
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
            GeneralConfig("", "", "", "")
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
            WarnPluginConfig(
                "echo Test1Test.java:4:6: Class name should be in PascalCase",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                true, true, 1, ", ", 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig("", "", "", "")
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
            defaultWarnConfig.copy(),
            GeneralConfig("", "", "", "")
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
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"} && set stub=",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                true, true, 1, ", ", 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig(
                "", "", "", "", expectedWarningsPattern = Regex("(.+):(\\d+):(\\d+): (.+)"),
            )
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
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"} && set stub=",
                Regex("// ;warn: (.*)"),
                false, false, 1, ", ", null, null, 1, 1, null, null, 2
            ), GeneralConfig(
                "", "", "", "", expectedWarningsPattern = Regex("(.+): (.+)"),
            )
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
            GeneralConfig("", "", "", "")
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
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(4, results.size)
            assertTrue(results.all { it.status is Pass })
        }
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

    @Test
    @Ignore
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
}

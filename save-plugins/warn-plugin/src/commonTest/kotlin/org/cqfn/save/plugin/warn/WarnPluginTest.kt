package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
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

/**
 * Needed tests:
 * - discovering of file pairs
 * - running tool
 */
class WarnPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPluginTest::class.simpleName!!)

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @Test
    fun `basic warn-plugin test`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """
                |Test1Test.java:4:6: Class name should be in PascalCase
                """.trimMargin().encodeToByteArray()
            )
        }
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 1, 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    fun `basic warn-plugin test with exactWarningsMatch = false`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """
                |Test1Test.java:4:6: Class name should be in PascalCase
                |Test1Test.java:5:8: Variable name should be in lowerCamelCase
                """.trimMargin().encodeToByteArray()
            )
        }
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 1, 1, 2, 3, 1, 2, 3, 4, false
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
            val nameWarn = "Some warnings were unexpected: [[Warning(message=Variable name should be in lowerCamelCase, line=5, column=8, fileName=Test1Test.java)]]"
            assertEquals((results.single().status as Pass).message, nameWarn)
        }
        fs.delete(tmpDir / "resource")
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
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 1, 1, 2, 3, 1, 2, 3, 4
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
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """Test1Test.java:1:1: Avoid using default package
                    |Test1Test.java:3:6: Class name should be in PascalCase
                    |Test1Test.java:5:5: Variable name should be in lowerCamelCase
                    |Test1Test.java:7:1: File should end with trailing newline
                    |""".trimMargin().encodeToByteArray()
            )
        }
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 1, 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    @Ignore  // this logic is todo
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test - multiple warnings & ignore technical comments`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """Test1Test.java:1:1: Avoid using default package
                    |Test1Test.java:3:6: Class name should be in PascalCase
                    |Test1Test.java:5:5: Variable name should be in lowerCamelCase
                    |Test1Test.java:7:1: File should end with trailing newline
                    |""".trimMargin().encodeToByteArray()
            )
        }
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
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 1, 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test - multiple warnings, no line-col`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """Test1Test.java: Avoid using default package
                    |Test1Test.java: Class name should be in PascalCase
                    |Test1Test.java: Variable name should be in lowerCamelCase
                    |Test1Test.java: File should end with trailing newline
                    |""".trimMargin().encodeToByteArray()
            )
        }
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
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn: (.*)"),
                Regex("(.+): (.+)"),
                false, false, 1, null, null, 1, 1, null, null, 2
            ), GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            results.single().status.let {
                assertTrue(it is Pass, "Expected test to pass, but actually got status $it")
            }
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `warn-plugin test for batchSize`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """
                |Test1Test.java:4:6: Class name should be in PascalCase
                |Test2Test.java:2:3: Class name should be in PascalCase
                """.trimMargin().encodeToByteArray()
            )
        }
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
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
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("(.+):(\\d+):(\\d+): (.+)"),
                true, true, 2, 1, 2, 3, 1, 2, 3, 4
            ),
            GeneralConfig("", "", "", "")
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    private fun performTest(
        texts: List<String>,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
        assertion: (List<TestResult>) -> Unit) {
        val config = fs.createFile(tmpDir / "save.toml")
        var i = 1
        texts.forEach {
            val testFileName = "Test${i++}Test.java"
            fs.write(fs.createFile(tmpDir / testFileName)) {
                write(it.encodeToByteArray())
            }
        }

        val results = WarnPlugin(TestConfig(config, null, mutableListOf(warnPluginConfig, generalConfig)))
            .execute()
            .toList()
        println(results)
        assertion(results)
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
    fun `warn-plugin test create test file`() {
        fs.write(fs.createFile(tmpDir / "resource")) {
            write(
                """
                package org.cqfn.save.example
                
                // ;warn:3:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent().encodeToByteArray()
            )
        }

        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
        val warnPluginConfig = WarnPluginConfig(
            "$catCmd ${tmpDir / "resource"}",
            Regex("// ;warn: (.*)"),
            Regex("(.+): (.+)"),
            false, false, 1, null, null, 1, 1, null, null, 2
        )
        val generalConfig = GeneralConfig("", "", "", "")
        val config = fs.createFile(tmpDir / "save.toml")
        val nameFile = WarnPlugin(TestConfig(config, null, mutableListOf(warnPluginConfig, generalConfig)))
            .createTestFile(tmpDir / "resource", warnPluginConfig.warningsInputPattern!!)
        val tmpDirTest = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)
        fs.readLines(tmpDirTest / nameFile).forEach {
            assertTrue(!warnPluginConfig.warningsInputPattern!!.matches(it))
        }
    }
}

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
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
            """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent(),
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3
            )
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
            """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int Foo = 42;
                }
            """.trimIndent(),
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3, false
            )
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
            val nameWarn = "Some warnings were unexpected: [[Warning(message=Variable name should be in lowerCamelCase, line=5, column=8)]]"
            assertEquals((results.single().status as Pass).message, nameWarn)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    @Ignore  // this logic is todo
    fun `basic warn-plugin test with ignoreTechnicalComments=true`() {
        performTest(
            """
                package org.cqfn.save.example
                
                // ;warn:3:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent(),
            WarnPluginConfig(
                "echo Test1Test.java:4:6: Class name should be in PascalCase",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"), Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3
            ),
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
    }

    @Test
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
            """
                // ;warn:1:1: Avoid using default package
                // ;warn:3:6: Class name should be in PascalCase
                class example {
                    // ;warn:5:5: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn:7:1: File should end with trailing newline
            """.trimIndent(),
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3
            )
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
    @Ignore  // this logic is todo
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
            """
                // ;warn:1:1: Avoid using default package
                // ;warn:1:6: Class name should be in PascalCase
                class example {
                    // ;warn:2:5: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn:3:1: File should end with trailing newline
            """.trimIndent(),
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3
            ),
        ) { results ->
            assertEquals(1, results.size)
            assertTrue(results.single().status is Pass)
        }
        fs.delete(tmpDir / "resource")
    }

    @Test
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
            """
                // ;warn: Avoid using default package
                // ;warn: Class name should be in PascalCase
                class example {
                    // ;warn: Variable name should be in lowerCamelCase
                    int Foo = 42;
                }
                // ;warn: File should end with trailing newline
            """.trimIndent(),
            WarnPluginConfig(
                "$catCmd ${tmpDir / "resource"}",
                Regex("// ;warn: (.*)"),
                Regex("[\\w\\d.-]+: (.+)"),
                false, false, null, null, 1
            )
        ) { results ->
            assertEquals(1, results.size)
            results.single().status.let {
                assertTrue(it is Pass, "Expected test to pass, but actually got status $it")
            }
        }
        fs.delete(tmpDir / "resource")
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    private fun performTest(
        text: String,
        warnPluginConfig: WarnPluginConfig,
        assertion: (List<TestResult>) -> Unit) {
        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        val config = fs.createFile(tmpDir / "save.toml")
        fs.write(testFile) {
            write(text.encodeToByteArray())
        }

        val results = WarnPlugin(TestConfig(config, null, mutableListOf(warnPluginConfig)))
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
                5,
                2,
                3
            )
        }
    }
}

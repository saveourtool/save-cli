package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.LanguageType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.utils.isCurrentOsWindows

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

private val mockConfig = SaveProperties(
    testConfig = ".",
    parallelMode = false,
    threads = 1,
    propertiesFile = ".",
    debug = true,
    quiet = false,
    reportType = ReportType.PLAIN,
    baseline = null,
    excludeSuites = null,
    includeSuites = null,
    language = LanguageType.KOTLIN,
    testRootPath = ".",
    resultOutput = ResultOutputType.STDOUT,
    configInheritance = true,
    ignoreSaveComments = false,
    reportDir = "."
)

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
        performTest(
            """
                package org.cqfn.save.example
                
                // ;warn:4:6: Class name should be in PascalCase
                class example {
                    int foo = 42;
                }
            """.trimIndent(),
            WarnPluginConfig(
                "echo Test1Test.java:4:6: Class name should be in PascalCase",
                Regex("// ;warn:(\\d+):(\\d+): (.*)"),
                Regex("[\\w\\d.-]+:(\\d+):(\\d+): (.+)"),
                true, true, 1, 2, 3
            )
        )
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
            mockConfig.copy(ignoreSaveComments = true)
        )
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
        )
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
            mockConfig.copy(ignoreSaveComments = true)
        )
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
        )
        fs.delete(tmpDir / "resource")
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    private fun performTest(
        text: String,
        warnPluginConfig: WarnPluginConfig,
        saveProperties: SaveProperties = mockConfig) {
        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        fs.write(testFile) {
            write(text.encodeToByteArray())
        }

        WarnPlugin().execute(
            saveProperties,
            TestConfig(".".toPath(), null, listOf(warnPluginConfig.copy(testResources = listOf(testFile))))
        )
    }
}

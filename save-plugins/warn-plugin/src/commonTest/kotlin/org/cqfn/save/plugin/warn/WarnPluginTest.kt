package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.LanguageType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
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

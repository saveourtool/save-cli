package org.cqfn.save.core

import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.files.MergeConfigs
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines

import io.github.petertrr.diffutils.diff
import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("TOO_LONG_FUNCTION", "LOCAL_VARIABLE_EARLY_DECLARATION")
class MergeConfigsTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / MergeConfigsTest::class.simpleName!!).also {
        fs.createDirectory(it)
    }
    private val configDetector = ConfigDetector()
    private val mergeConfigs = MergeConfigs()

    @Test
    fun `merge two configs`() {
        val tomlTemplate1 = """
                |[general]
                |description = "Description 1"
                |suiteName = "DocsCheck"
                |
                |[warn]
                |execCmd = "./detekt --build-upon-default-config -i"
                |
                |[fix]
                |execCmd = fixCmd
                """

        val tomlTemplate2 = """
                |[general]
                |description = "Description 2"
                |
                |[warn]
                |execCmd = "someCmd"
                """

        val expectedToml = """
                |[general]
                |description = "Description 2"
                |suiteName = "DocsCheck"
                |
                |[warn]
                |execCmd = "someCmd"
                |
                |[fix]
                |execCmd = fixCmd
                """

        val toml1 = fs.createFile(tmpDir / "save.toml")
        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")
        val expectedFile = fs.createFile(tmpDir / "Expected.toml")

        fs.write(toml1) {
            write(tomlTemplate1.trimMargin().encodeToByteArray())
        }

        fs.write(toml2) {
            write(tomlTemplate2.trimMargin().encodeToByteArray())
        }

        fs.write(expectedFile) {
            write(expectedToml.trimMargin().encodeToByteArray())
        }

        val result = configDetector.configFromFile(toml2.toString())
        mergeConfigs.merge(result)

        assertTrue("Files should be identical") {
            diff(fs.readLines(toml2), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @Test
    fun `merge many configs`() {
        // Will take [fix] table from this file
        val tomlTemplate1 = """
                |[general]
                |description = "Description 1"
                |
                |[fix]
                |execCmd = fixCmd
                """

        // Will take [suiteName] and [warningsInputPattern] values from this file
        val tomlTemplate2 = """
                |[general]
                |description = "Description 2"
                |suiteName = "DocsCheck"
                |
                |[warn]
                |warningsInputPattern = "// ;warn:(\\d+):(\\d+): (.*)"
                """

        // Will take [execCmd] value from this file
        val tomlTemplate3 = """
                |[general]
                |description = Description 3"
                |
                |[warn]
                |execCmd = "someExecCmd"
                """
        // Config which should be merged
        val tomlTemplate4 = """
                |[general]
                |description = "Description 4"
                |
                |[warn]
                |warningTextHasColumn = true
                |warningTextHasLine = true
                """

        val expectedToml = """
                |[general]
                |description = "Description 4"
                |suiteName = "DocsCheck"
                |
                |[warn]
                |warningTextHasColumn = true
                |warningTextHasLine = true
                |execCmd = "someExecCmd"
                |warningsInputPattern = "// ;warn:(\\d+):(\\d+): (.*)"
                |
                |[fix]
                |execCmd = fixCmd
                """

        val toml1 = fs.createFile(tmpDir / "save.toml")

        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")

        val nestedDir2 = tmpDir / "nestedDir1" / "nestedDir2"
        fs.createDirectory(nestedDir2)
        val toml3 = fs.createFile(nestedDir2 / "save.toml")

        fs.createDirectory(nestedDir2 / "nestedDir3")
        fs.createDirectory(nestedDir2 / "nestedDir3" / "nestedDir4")
        val toml4 = fs.createFile(nestedDir2 / "nestedDir3" / "nestedDir4" / "save.toml")

        fs.write(toml1) {
            write(tomlTemplate1.trimMargin().encodeToByteArray())
        }

        fs.write(toml2) {
            write(tomlTemplate2.trimMargin().encodeToByteArray())
        }

        fs.write(toml3) {
            write(tomlTemplate3.trimMargin().encodeToByteArray())
        }

        fs.write(toml4) {
            write(tomlTemplate4.trimMargin().encodeToByteArray())
        }

        val result = configDetector.configFromFile(toml4.toString())
        mergeConfigs.merge(result)

        val expectedFile = tmpDir / "Expected.toml"
        fs.write((fs.createFile(expectedFile))) {
            write(expectedToml.trimMargin().encodeToByteArray())
        }

        assertTrue("Files should be identical") {
            diff(fs.readLines(toml4), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}

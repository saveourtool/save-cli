package com.saveourtool.save.plugin.warn.sarif

import com.saveourtool.save.core.files.getWorkingDirectory
import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.plugin.warn.utils.Warning

import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Message
import io.github.detekt.sarif4k.PhysicalLocation
import io.github.detekt.sarif4k.Result
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import okio.Path
import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val sp = Path.DIRECTORY_SEPARATOR

class SarifWarningAdapterTest {
    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `should convert SARIF report to SAVE warnings`() {
        val sarif = """
            {
              "version": "2.1.0",
              "${'$'}schema": "http://json.schemastore.org/sarif-2.1.0-rtm.4",
              "runs": [
                {
                  "tool": {
                    "driver": {
                      "name": "ESLint",
                      "informationUri": "https://eslint.org",
                      "rules": [
                        {
                          "id": "no-unused-vars",
                          "shortDescription": {
                            "text": "disallow unused variables"
                          },
                          "helpUri": "https://eslint.org/docs/rules/no-unused-vars"
                        }
                      ]
                    }
                  },
                  "artifacts": [
                    {
                      "location": {
                        "uri": "file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js"
                      }
                    }
                  ],
                  "results": [
                    {
                      "level": "error",
                      "message": {
                        "text": "'x' is assigned a value but never used."
                      },
                      "locations": [
                        {
                          "physicalLocation": {
                            "artifactLocation": {
                              "uri": "file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js",
                              "index": 0
                            },
                            "region": {
                              "startLine": 1,
                              "startColumn": 5
                            }
                          }
                        }
                      ],
                      "ruleId": "no-unused-vars",
                      "ruleIndex": 0
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val sarifSchema210: SarifSchema210 = Json.decodeFromString(sarif)

        val warnings = sarifSchema210.toWarnings("C:/dev/sarif".toPath(), emptyList(), getWorkingDirectory())

        logInfo("Converted warnings: $warnings")
        assertEquals(1, warnings.size)
        assertEquals(
            Warning("'x' is assigned a value but never used.", 1, 5, "simple-example.js"),
            warnings.first()
        )
    }

    @Test
    fun `should filter out warnings from other files`() {
        val sarifSchema210 = SarifSchema210(
            version = Version.The210,
            runs = listOf(
                runOf(uri = "file:///workspace/tests/suite1/foo.test"),
                runOf(uri = "file:///workspace/tests/suite1/bar.test"),
                runOf(uri = "file:///workspace/tests/suite2/foo.test"),
            )
        )

        val testRoot = "${sp}workspace${sp}tests".toPath()
        val warnings = sarifSchema210.toWarnings(
            testRoot,
            listOf("${sp}workspace${sp}tests${sp}suite2${sp}foo.test".toPath()).adjustToCommonRoot(testRoot),
            getWorkingDirectory()
        )

        logInfo("Converted warnings: $warnings")
        assertEquals(1, warnings.size)
    }

    @Test
    fun `should filter out warnings from other files - relative paths`() {
        val sarifSchema210 = SarifSchema210(
            version = Version.The210,
            runs = listOf(
                runOf(uri = "suite1/foo.test"),
                runOf(uri = "suite1/bar.test"),
                runOf(uri = "suite2/foo.test"),
            )
        )

        val testRoot = "${sp}workspace${sp}tests".toPath()
        val warnings = sarifSchema210.toWarnings(
            testRoot,
            listOf("${sp}workspace${sp}tests${sp}suite2${sp}foo.test".toPath()).adjustToCommonRoot(testRoot),
            getWorkingDirectory()
        )

        logInfo("Converted warnings: $warnings")
        assertEquals(1, warnings.size)
    }

    @Test
    fun `should filter out warnings from other files - absolute paths, testRoot relative`() {
        val warnings = extractWarningsWithAbsolutePathsFromSarif("tests".toPath())
        assertEquals(1, warnings.size)
    }

    @Test
    fun `should filter out warnings from other files - absolute paths, testRoot absolute`() {
        val warnings = extractWarningsWithAbsolutePathsFromSarif("${getWorkingDirectory()}${sp}tests".toPath())
        assertEquals(1, warnings.size)
    }
}

private fun extractWarningsWithAbsolutePathsFromSarif(testRoot: Path): List<Warning> {
    val workingDir = getWorkingDirectory()
    val sarifSchema210 = SarifSchema210(
        version = Version.The210,
        runs = listOf(
            runOf(uri = "$workingDir${sp}tests${sp}suite1${sp}foo.test"),
            runOf(uri = "$workingDir${sp}tests${sp}suite2${sp}foo.test"),
        )
    )

    return sarifSchema210.toWarnings(
        testRoot,
        listOf("suite2${sp}foo.test".toPath()),
        workingDir
    ).also { logInfo("Converted warnings: $it") }
}

private fun runOf(message: Message = Message(), uri: String) = Run(
    tool = Tool(driver = ToolComponent(name = "unit-test")),
    results = listOf(
        Result(
            locations = listOf(
                Location(
                    physicalLocation = PhysicalLocation(
                        artifactLocation = ArtifactLocation(
                            uri = uri
                        )
                    )
                )
            ),
            message = message,
        )
    )
)

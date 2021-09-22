package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat

import kotlin.test.Ignore
import kotlin.test.Test

// To run these tests locally on your Native platforms you would need to install curl for your OS:
// On Windows you'll also need to install msys2 and run pacman -S mingw-w64-x86_64-curl to have libcurl for ktor-client.
// On ubuntu install libcurl4-openssl-dev for ktor client and libncurses5 for kotlin/native compiler.

/**
 * testing that save can successfully work with:
 * 1) separate files
 * 2) save.toml config
 * 3) test directory
 * 4) root path directory
 */
class ClassicWarnTest {
    @Test
    @Ignore  // fixme: change directory in fix-plugin too?
    fun `execute warn plugin with default testFiles`() =
            runTestsWithDiktat(
                null, 9
            )

    @Test
    fun `execute warn plugin on separate files`() =
            runTestsWithDiktat(
                listOf(
                    "warn/chapter1/EnumValueSnakeCaseTest.kt",
                    "warn/chapter1/GenericFunctionTest.kt"
                ), 2
            )

    @Test
    fun `executing warn plugin on directory`() =
            runTestsWithDiktat(
                listOf(
                    "warn/chapter1"
                ), 3
            )

    @Test
    fun `executing warn plugin on save-toml file in directory`() =
            runTestsWithDiktat(
                listOf(
                    "warn/save.toml"
                ), 3
            )

    @Test
    fun `executing warn plugin on parental save-toml file`() =
            runTestsWithDiktat(
                listOf(
                    "warn/chapter1/save.toml"
                ), 3
            )

    @Test
    fun `execute warn plugin with included and excluded suites`() =
            runTestsWithDiktat(
                emptyList(), 1
            ) {
                includeSuites = "Autofix and Warn,Unknown name"
                excludeSuites = "Directory: Chapter1,Directory: Chapter2,Unknown name"
            }

    @Test
    fun `tests should have a relative path`() =
            runTestsWithDiktat(
                listOf(
                    "EnumValueSnakeCaseTest.kt",
                    "GenericFunctionTest.kt"
                ), 0
            )
}

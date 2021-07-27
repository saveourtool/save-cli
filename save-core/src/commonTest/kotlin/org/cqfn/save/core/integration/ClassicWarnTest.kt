package org.cqfn.save.core.integration

import kotlin.test.Test
import org.cqfn.save.core.test.utils.runTest
import org.cqfn.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Ignore

// To run these tests locally on your Native platforms you would need to install curl for your OS:
// On windows you'll also need to install msys2 and run pacman -S mingw-w64-x86_64-curl to have libcurl for ktor-client.
// On ubuntu install libcurl4-openssl-dev for ktor client and libncurses5 for kotlin/native compiler.
const val KTLINT_VERSION = "0.39.0"
const val DIKTAT_VERSION = "1.0.0-rc.2"

/**
 * testing that save can successfully work with:
 * 1) separate files
 * 2) save.toml config
 * 3) test directory
 * 4) root path directory
 */
class ClassicWarnTest {
    @Test
    fun `execute warn plugin on separate files`() = runTest {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/EnumValueSnakeCaseTest.kt",
                "warn/chapter1/GenericFunctionTest.kt"
            ), 2
        )
    }

    @Test
    fun `executing warn plugin on directory`() = runTest {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1"
            ), 2
        )
    }

    @Ignore
    // FixMe: this test is failing right now - should be investigated
    @Test
    fun `executing warn plugin on save-toml file in directory`() = runTest {
        runTestsWithDiktat(
            listOf(
                "warn/save.toml"
            ), 2
        )
    }

    @Ignore
    // FixMe: this test is failing right now - should be investigated
    @Test
    fun `executing warn plugin on parental save-toml file`() = runTest {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/save.toml"
            ), 2
        )
    }
}

package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat

import kotlin.test.Test

// To run these tests locally on your Native platforms you would need to install curl for your OS:
// On windows you'll also need to install msys2 and run pacman -S mingw-w64-x86_64-curl to have libcurl for ktor-client.
// On ubuntu install libcurl4-openssl-dev for ktor client and libncurses5 for kotlin/native compiler.

class WarnDirTest {
    @Test
    fun `execute warn plugin on the directory chapter1`() =
            runTestsWithDiktat(listOf("warn-dir/chapter1"), 3)

    @Test
    fun `execute warn plugin on the directory chapter2`() =
            runTestsWithDiktat(listOf("warn-dir/chapter2/GenericFunctionTest.kt"), 1)

    @Test
    fun `execute warn plugin on the directory chapter3`() =
            runTestsWithDiktat(listOf("warn-dir/chapter3"), 1)
}

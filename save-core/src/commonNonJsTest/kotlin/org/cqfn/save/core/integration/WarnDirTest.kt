package org.cqfn.save.core.integration

import org.cqfn.save.AfterClass
import org.cqfn.save.BeforeClass
import org.cqfn.save.core.test.utils.runTest
import org.cqfn.save.core.test.utils.runTestsWithDiktat

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.jvm.JvmStatic
import kotlin.test.Ignore
import kotlin.test.Test

// To run these tests locally on your Native platforms you would need to install curl for your OS:
// On windows you'll also need to install msys2 and run pacman -S mingw-w64-x86_64-curl to have libcurl for ktor-client.
// On ubuntu install libcurl4-openssl-dev for ktor client and libncurses5 for kotlin/native compiler.

class WarnDirTest {
    @Test
    fun `execute warn plugin on the directory chapter1`() = runTest {
        runTestsWithDiktat(listOf("warn-dir/chapter1"), 3)
    }

    @Test
    // FixMe: this test will fail until we will support proper calculation of line number in the default mode
    // FixMe: to do that we need: 1) to exclude warning comments from the initial code, so it won't rigger warns
    // FixMe: 2) to merge the logic for EXPLICIT set of line numbers with the default mode to provide line numbers like "1"
    @Ignore
    fun `execute warn plugin on the directory chapter2`() = runTest {
        runTestsWithDiktat(listOf("warn-dir/chapter2"), 1)
    }

    @Test
    fun `execute warn plugin on the directory chapter3`() = runTest {
        runTestsWithDiktat(listOf("warn-dir/chapter3"), 1)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            numTestsRunning.addAndGet(1)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            if (numTestsRunning.addAndGet(-1) == 0) {
                FileSystem.SYSTEM.delete("../examples/kotlin-diktat/ktlint".toPath())
                FileSystem.SYSTEM.delete("../examples/kotlin-diktat/diktat.jar".toPath())
            }
        }
    }
}

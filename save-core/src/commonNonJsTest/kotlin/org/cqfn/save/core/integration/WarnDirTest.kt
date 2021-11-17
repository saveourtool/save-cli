package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat

import kotlin.test.Test

class WarnDirTest {
    @Test
    fun `execute warn plugin on the directory chapter1`() {
        runTestsWithDiktat(listOf("warn-dir/chapter1"), 3)
    }

    @Test
    fun `execute warn plugin on the directory chapter2`() {
        runTestsWithDiktat(listOf("warn-dir/chapter2"), 1)
    }

    @Test
    fun `execute warn plugin on the directory chapter3`() {
        runTestsWithDiktat(listOf("warn-dir/chapter3"), 1)
    }
}

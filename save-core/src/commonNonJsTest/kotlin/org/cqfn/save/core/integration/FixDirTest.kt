package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test

class FixDirTest {
    @Test
    fun `execute fix plugin`() =
            runTestsWithDiktat(listOf("fix/smoke/src/main/kotlin/org/cqfn/save"), 2)

    @Test
    fun `execute fix plugin on the directory chapter1`() =
            runTestsWithDiktat(listOf("fix/smoke/src/main/kotlin/org/cqfn/save/chapter1"), 1)
}

package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test

class FixAndWarnDirTest {
    @Test
    fun `execute fix-and-warn plugin on the directory chapter1`() =
            runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter1"), 1)

    @Test
    fun `execute fix-and-warn plugin on the directory chapter2`() =
            runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter2"), 1)

    @Test
    fun `execute fix-and-warn plugin on the directory chapter3`() =
            runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter3"), 1)
}

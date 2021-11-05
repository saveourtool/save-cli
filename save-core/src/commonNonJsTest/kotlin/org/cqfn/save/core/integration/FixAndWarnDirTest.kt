package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test
import kotlin.test.assertTrue

class FixAndWarnDirTest {
    @Test
    fun `execute fix-and-warn plugin on the directory chapter1`() {
        runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter1"), 1)
    }

    @Test
    fun `execute fix-and-warn plugin on the directory chapter2`() {
        runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter2"), 1)
    }

    @Test
    @Suppress("COMPLEX_EXPRESSION")
    fun `execute fix-and-warn plugin on the directory chapter3`() {
        val testReport = runTestsWithDiktat(listOf("fix_and_warn/smoke/src/main/kotlin/org/cqfn/save/chapter3"), 1)
        testReport.results.map { test ->
            assertTrue(test.resources.test.name.contains("Test"))
        }
    }
}

package com.saveourtool.save.core.integration

import com.saveourtool.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test

class FixDirTest {
    @Test
    fun `execute fix plugin`() {
        runTestsWithDiktat(listOf("fix/smoke/src/main/kotlin/com/saveourtool/save"), 5)
    }

    @Test
    fun `execute fix plugin on the directory chapter1`() {
        runTestsWithDiktat(listOf("fix/smoke/src/main/kotlin/com/saveourtool/save/chapter1"), 1)
    }
}

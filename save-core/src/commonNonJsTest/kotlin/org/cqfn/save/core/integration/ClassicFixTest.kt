package org.cqfn.save.core.integration

import org.cqfn.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test

class ClassicFixTest {
    @Test
    fun `execute fix plugin on separate files`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/org/cqfn/save/Example1Test.kt",
                "fix/smoke/src/main/kotlin/org/cqfn/save/Example1Expected.kt"
            ), 1
        )
    }

    @Test
    fun `executing fix plugin on save-toml file in directory`() {
        runTestsWithDiktat(
            listOf(
                "fix/save.toml"
            ), 2
        )
    }

    @Test
    fun `executing fix plugin on parental save-toml file`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/save.toml"
            ), 2
        )
    }
}

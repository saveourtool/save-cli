package com.saveourtool.save.core.integration

import com.saveourtool.save.core.test.utils.runTestsWithDiktat
import kotlin.test.Test

class ClassicFixTest {
    @Test
    fun `execute fix plugin on separate files`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/Example1Test.kt",
                "fix/smoke/src/main/kotlin/com/saveourtool/save/Example1Expected.kt"
            ), 1
        )
    }

    @Test
    fun `executing fix plugin on save-toml file in directory`() {
        runTestsWithDiktat(
            listOf(
                "fix/save.toml"
            ), 7
        )
    }

    @Test
    fun `executing fix plugin on parental save-toml file`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/save.toml"
            ), 6
        )
    }

    @Test
    fun `execute fix plugin on folder`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/"
            ), 6
        )
    }

    @Test
    fun `check NoIgnoreLines`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/IgnoreLinesTest/NoIgnoreLines"
            ), 1
        )
    }

    @Test
    fun `check IgnoreLinesIsEmpty`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/IgnoreLinesTest/IgnoreLinesIsEmpty"
            ), 1
        )
    }

    @Test
    fun `check IgnoreLines`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/IgnoreLinesTest/IgnoreLines"
            ), 1
        )
    }

    @Test
    fun `execute fix plugin in sarif mode`() {
        runTestsWithDiktat(
            listOf(
                "fix/sarif/src/main/kotlin/com/saveourtool/save"
            ), 1
        )
    }

    @Test
    fun `tests with the same name`() {
        runTestsWithDiktat(
            listOf(
                "fix/smoke/src/main/kotlin/com/saveourtool/save/Example1Test.kt",
                "fix/smoke/src/main/kotlin/com/saveourtool/save/Example1Expected.kt",
                "fix/smoke/src/main/kotlin/com/saveourtool/save/chapter2/Example1Test.kt",
                "fix/smoke/src/main/kotlin/com/saveourtool/save/chapter2/Example1Expected.kt",
            ), 2
        )
    }
}

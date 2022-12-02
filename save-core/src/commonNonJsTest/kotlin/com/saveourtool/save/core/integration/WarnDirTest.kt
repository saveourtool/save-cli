package com.saveourtool.save.core.integration

import com.saveourtool.save.core.test.utils.runTestsWithDiktat
import io.kotest.matchers.collections.exist
import io.kotest.matchers.shouldNot

import kotlin.test.Ignore
import kotlin.test.Test

@Ignore  // https://github.com/saveourtool/save-cli/issues/402
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

    @Test
    fun `execute warn plugin on the directory chapter4`() {
        runTestsWithDiktat(listOf("warn-dir/chapter4"), 1)
    }

    @Test
    fun `execute warn plugin on root directory`() {
        val reporter = runTestsWithDiktat(listOf("warn-dir"), 6)
        reporter.results shouldNot exist { testResult ->
            testResult.debugInfo!!.execCmd!!.contains("chapter")
        }
    }
}

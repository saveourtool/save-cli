package com.saveourtool.save.core

import com.saveourtool.save.core.config.ReportType
import com.saveourtool.save.core.config.SaveProperties

import okio.FileSystem

import kotlin.test.Test

class PerformAnalysisTest {
    private val fs: FileSystem = FileSystem.SYSTEM

    @Test
    fun `detect plugins`() {
        val saveProperties = SaveProperties(
            reportType = ReportType.PLAIN,
            testRootDir = "../examples/discovery-test",
            testFiles = emptyList()
        )
        // In this test we need to merge with emulated empty save.properties file in aim to use default values,
        // since initially all fields are null
        Save(saveProperties, fs).performAnalysis()
    }

    @Test
    fun `should execute single test`() {
        val saveProperties = SaveProperties(
            reportType = ReportType.PLAIN,
            testRootDir = "../examples/discovery-test",
            testFiles = listOf("../discovery-test/highlevel/suite1/MyTest.java")
        )
        Save(saveProperties, fs).performAnalysis()
        // fixme: check that only a single test has been executed
    }
}

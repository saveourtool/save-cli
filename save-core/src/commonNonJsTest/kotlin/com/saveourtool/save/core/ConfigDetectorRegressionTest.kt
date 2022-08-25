package com.saveourtool.save.core

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.files.ConfigDetector
import com.saveourtool.save.core.files.fs

import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigDetectorRegressionTest {
    private val evaluatedToolConfig = EvaluatedToolConfig(1, "")

    @Test
    fun `config detector regression test on directories`() {
        val baseDir = "../examples/discovery-test"
        val expected = listOf(
            "$baseDir/save.toml", "$baseDir/highlevel/save.toml",
            "$baseDir/highlevel/suite1/save.toml", "$baseDir/highlevel/suite1/subSuite/save.toml",
            "$baseDir/highlevel/suite2/inner/save.toml"
        )

        val actual1 = ConfigDetector(fs, evaluatedToolConfig, emptyList())
            .configFromFile(baseDir.toPath())
            .getAllTestConfigs()
            .map { it.location.toString() }

        assertEquals(expected, actual1)

        val actual2 = ConfigDetector(fs, evaluatedToolConfig, emptyList())
            .configFromFile("$baseDir/save.toml".toPath())
            .getAllTestConfigs()
            .map { it.location.toString() }

        assertEquals(expected, actual2)
    }
}

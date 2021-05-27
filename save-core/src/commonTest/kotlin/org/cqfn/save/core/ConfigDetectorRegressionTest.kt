package org.cqfn.save.core

import okio.Path.Companion.toPath
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigDetectorRegressionTest {
    @Test
    fun `config detector regression test on directories`() {
        val expected = listOf(
            "../examples/save.toml", "../examples/highlevel/save.toml",
            "../examples/highlevel/suite1/save.toml", "../examples/highlevel/suite2/inner/save.toml"
        )

        val actual1 = ConfigDetector()
            .configFromFile("../examples".toPath())
            .getAllTestConfigs()
            .map { it.location.toString() }

        assertEquals(expected, actual1)

        val actual2 = ConfigDetector()
            .configFromFile("../examples/save.toml".toPath())
            .getAllTestConfigs()
            .map { it.location.toString() }

        assertEquals(expected, actual2)
    }
}

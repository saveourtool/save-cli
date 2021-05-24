package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import kotlin.test.Test

class PluginDiscoveryTest {
    @Test
    fun `detect plugins`() {
        val saveProperties = SaveProperties(
            testRootPath = "../examples",
        )

        Save(saveProperties).performAnalysis()
    }
}

package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import kotlin.test.Test

class PluginDiscoveryTest {
    val saveProperties = SaveProperties(
        testRootPath = "../examples",
    )

    @Test
    fun `detect plugins`() {
        Save(saveProperties).performAnalysis()
    }
}

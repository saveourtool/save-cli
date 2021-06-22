package org.cqfn.save.core

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import kotlin.test.Test

class PerformAnalysisTest {
    @Test
    fun `detect plugins`() {
        val saveProperties = SaveProperties(
            testRootPath = "../examples/discovery-test",
            reportType = ReportType.PLAIN,
        )
        // In this test we need to merge with emulated empty save.properties file in aim to use default values,
        // since initially all fields are null
        Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties())).performAnalysis()
    }
}

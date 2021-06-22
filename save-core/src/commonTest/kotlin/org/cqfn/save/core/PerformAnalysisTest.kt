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

        Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties())).performAnalysis()
    }
}

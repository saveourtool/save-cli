package org.cqfn.save.core

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import kotlin.test.Test

class PerformAnalysisTest {
    @Test
    fun `detect plugins`() {
        val saveProperties = SaveProperties(
            testRootPath = "../examples/kotlin-diktat",
            reportType = ReportType.PLAIN,
        )

        Save(saveProperties).performAnalysis()
    }
}

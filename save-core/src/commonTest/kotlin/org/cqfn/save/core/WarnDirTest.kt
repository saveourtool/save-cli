package org.cqfn.save.core

import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import kotlin.test.Test

class WarnDirTest {
    @Test
    fun `execute warn plugin on directory`() {
        val saveProperties = SaveProperties(
            testRootPath = "../examples/kotlin-diktat/warn-dir",
            reportType = ReportType.JSON,
            resultOutput = OutputStreamType.FILE
        )
        // In this test we need to merge with emulated empty save.properties file in aim to use default values,
        // since initially all fields are null
        Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties())).performAnalysis()
    }
}

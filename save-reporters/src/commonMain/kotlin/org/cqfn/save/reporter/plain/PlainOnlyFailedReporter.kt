package org.cqfn.save.reporter.plain

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult

import okio.BufferedSink

/**
 * A variation of [PlainTextReporter], which prints all events except for [Pass]es
 */
class PlainOnlyFailedReporter(out: BufferedSink) : PlainTextReporter(out) {
    override val type: ReportType = ReportType.PLAIN_FAILED

    override fun onEvent(event: TestResult) {
        if (event.status is Pass) {
            return
        } else {
            super.onEvent(event)
        }
    }
}

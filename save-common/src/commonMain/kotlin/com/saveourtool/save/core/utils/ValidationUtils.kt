/**
 * This file contains utils for validation
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.config.TestConfig

/**
 * @param testConfig
 * @param evaluatedToolConfig
 * @return execFlags
 */
fun String?.validateAndGetExecFlags(testConfig: TestConfig, evaluatedToolConfig: EvaluatedToolConfig): String {
    if (this != null) {
        testConfig.parentConfigs(true)
            .mapNotNull { it.getGeneralConfig() }
            .firstOrNull { it.execCmd != null }
            .let {
                requireNotNull(it) {
                    "`execCmd` should be set for tests with `execFlags`"
                }
            }

    }
    return evaluatedToolConfig.execFlags ?: this ?: ""
}

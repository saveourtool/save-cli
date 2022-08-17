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
        val generalConfig = requireNotNull(testConfig.getGeneralConfig()) {
            "Not found general config"
        }
        require(generalConfig.execCmd != null) {
            "`execCmd` should be set for tests with `execFlags`"
        }
    }
    return evaluatedToolConfig.execFlags ?: this ?: ""
}

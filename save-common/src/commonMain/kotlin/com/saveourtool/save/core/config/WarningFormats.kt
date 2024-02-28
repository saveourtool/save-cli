/**
 * Classes that describe format of expected and actual warnings
 */

package com.saveourtool.save.core.config

/**
 * Possible formats of actual (i.e. produces by the tool) warnings
 */
enum class ActualWarningsFormat {
    PLAIN,
    SARIF,
    ;
}

/**
 * Possible formats of expected (i.e. the ones used to check the tool) warnings
 */
enum class ExpectedWarningsFormat {
    IN_PLACE,
    PLAIN,
    SARIF,
    ;
}

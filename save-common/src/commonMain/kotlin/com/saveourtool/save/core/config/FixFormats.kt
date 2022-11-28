/**
 * Classes that describe format of expected and actual fixes
 */

package com.saveourtool.save.core.config

/**
 * Possible formats of actual (i.e. produces by the tool) warnings
 */
enum class ActualFixFormat {
    PLAIN,
    SARIF,
    ;
}

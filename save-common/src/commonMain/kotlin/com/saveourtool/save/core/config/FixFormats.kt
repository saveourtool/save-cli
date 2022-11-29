/**
 * Classes that describe format of expected and actual fixes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.core.config

/**
 * Possible formats of actual set of fixes, provided by user
 */
enum class ActualFixFormat {
    IN_PLACE,
    SARIF,
    ;
}

package com.saveourtool.save.core.config

/**
 * An interface for interim result during reading from TOML file
 *
 * @param I class represents interim result
 * @param R class represents result
 */
interface Interim<R, I : Interim<R, I>> {
    /**
     * @param overrides
     * @return result with values are overridden by values from [overrides]
     */
    fun merge(overrides: I) : I

    /**
     * @return result with values from interim object
     */
    fun build(): R
}
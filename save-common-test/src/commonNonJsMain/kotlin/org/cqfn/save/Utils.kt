/**
 * Common utilities for testing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save

import okio.FileSystem
import okio.Path
import kotlin.random.Random

/**
 * Marks method to be executed before all tests, supported on JVM and Native
 */
expect annotation class BeforeClass

/**
 * Marks method to be executed after all tests, supported on JVM and Native
 */
expect annotation class AfterClass

/**
 * Create a temporary directory
 *
 * @param prefix will be prepended to directory name
 * @return a [Path] representing the created directory
 */
fun FileSystem.createTempDir(prefix: String = "save-tmp"): Path {
    val dirName = "$prefix-${Random.nextInt()}"
    return (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / dirName).also {
        createDirectory(it)
    }
}

/**
 * Utility methods to format stuff to string
 */

package org.cqfn.save.core.logging

/**
 * Produces a string with [this] [Throwable]'s class and message
 *
 * @return a string with Throwable's description
 */
fun Throwable.describe(): String = "${this::class.simpleName}: $message"

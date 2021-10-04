/**
 * Various utility methods
 */

package org.cqfn.save.core.utils

/**
 * If [predicate] evaluates to `true` on `this`, execute [action] and return it's result.
 * Otherwise, return `this`.
 *
 * @param predicate a predicate on `this`
 * @param action an action to transform `this`
 */
inline fun <reified T : Any?> T.runIf(predicate: T.() -> Boolean, action: T.() -> T) =
        if (predicate(this)) action(this) else this

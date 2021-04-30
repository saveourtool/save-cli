package org.cqfn.save.core.reporter

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.TestResult

/**
 * A base interface for all reporters, that are capable of formatting results of SAVE execution
 */
@Suppress("USE_INLINE_CLASS")
interface Reporter {
    /**
     * A type of this reporter
     */
    val type: ReportType

    /**
     * Writes formatted text in a desired way, e.g. to stdout or into a file
     *
     * @param text a string to write
     */
    fun write(text: String)

    /**
     * This function is called before any tests are executed
     */
    fun beforeAll() {
        write("Initializing reporter ${this::class.qualifiedName} of type $type")
    }

    /**
     * This function is called after all tests are executed
     */
    fun afterAll()

    /**
     * This function is called whenever a new [TestResult] is available
     *
     * @param event an instance of [TestResult]
     */
    fun onEvent(event: TestResult)

    /**
     * This function is called when plugin is getting instantiated
     *
     * @param plugin a [Plugin]
     */
    fun onPluginInitialization(plugin: Plugin)

    /**
     * This function is called when plugin starts execution
     *
     * @param plugin a [Plugin]
     */
    fun onPluginExecutionStart(plugin: Plugin)

    /**
     * This function is called when plugin finishes execution
     *
     * @param plugin a [Plugin]
     */
    fun onPluginExecutionEnd(plugin: Plugin)

    /**
     * This function is called when an exception is thrown during plugin execution
     *
     * @param ex an [Exception] that has been thrown during plugin execution
     */
    fun onPluginExecutionError(ex: Exception)  // todo: change to proper exception class when it's introduced
}

/**
 * Calls [Reporter.beforeAll] on all elements in [this] collection
 */
fun Collection<Reporter>.beforeAll(): Unit = forEach { it.beforeAll() }

/**
 * Calls [Reporter.afterAll] on all elements in [this] collection
 */
fun Collection<Reporter>.afterAll(): Unit = forEach { it.afterAll() }

/**
 * Calls [Reporter.onEvent] on all elements in [this] collection
 *
 * @param event
 */
fun Collection<Reporter>.onEvent(event: TestResult): Unit = forEach { it.onEvent(event) }

/**
 * Calls [Reporter.onPluginInitialization] on all elements in [this] collection
 *
 * @param plugin
 */
fun Collection<Reporter>.onPluginInitialization(plugin: Plugin): Unit = forEach { it.onPluginInitialization(plugin) }

/**
 * Calls [Reporter.onPluginExecutionStart] on all elements in [this] collection
 *
 * @param plugin
 */
fun Collection<Reporter>.onPluginExecutionStart(plugin: Plugin): Unit = forEach { it.onPluginExecutionStart(plugin) }

/**
 * Calls [Reporter.onPluginExecutionEnd] on all elements in [this] collection
 *
 * @param plugin
 */
fun Collection<Reporter>.onPluginExecutionEnd(plugin: Plugin): Unit = forEach { it.onPluginExecutionEnd(plugin) }

/**
 * Calls [Reporter.onPluginExecutionError] on all elements in [this] collection
 *
 * @param ex
 */
fun Collection<Reporter>.onPluginExecutionError(ex: Exception): Unit = forEach { it.onPluginExecutionError(ex) }

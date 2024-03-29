package com.saveourtool.save.core.reporter

import com.saveourtool.save.core.config.ReportType
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.core.result.TestResult

import okio.BufferedSink

/**
 * A base interface for all reporters, that are capable of formatting results of SAVE execution
 */
interface Reporter {
    /**
     * A type of this reporter
     */
    val type: ReportType

    /**
     * A [Sink] into which this reporter should direct all it's output. Can represent e.g. stdout or a file.
     */
    val out: BufferedSink

    /**
     * This function is called before any tests are executed
     */
    fun beforeAll()

    /**
     * This function is called after all tests are executed
     */
    fun afterAll()

    /**
     * This function is called on test suite execution start
     *
     * @param suiteName test suite name
     */
    fun onSuiteStart(suiteName: String)

    /**
     * This function is called on test suite execution finish
     *
     * @param suiteName test suite name
     */
    fun onSuiteEnd(suiteName: String)

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
    fun onPluginExecutionError(ex: PluginException)
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
fun Collection<Reporter>.onPluginExecutionError(ex: PluginException): Unit = forEach { it.onPluginExecutionError(ex) }

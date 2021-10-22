/**
 * Configuration classes for SAVE plugins.
 */

@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.utils.RegexSerializer

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

/**
 * Core interface for plugin configuration (like warnPlugin/fixPluin/e.t.c)
 */
interface PluginConfig {
    /**
     * type of the config (usually related to the class: WARN/FIX/e.t.c)
     */
    val type: TestConfigSections

    /**
     * list of regexes to be ignored
     */
    var ignoreLinesPatterns: MutableList<Regex>

    /**
     * Location of the toml config
     */
    var configLocation: Path

    /**
     * @param otherConfig - 'this' will be merged with 'other'
     * @return merged config
     */
    fun mergeWith(otherConfig: PluginConfig): PluginConfig

    /**
     * Method, which validates config and provides the default values for fields, if possible
     *
     * @return new validated instance obtained from [this]
     */
    fun validateAndSetDefaults(): PluginConfig
}

/**
 * General configuration for test suite.
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execCmd a command that will be executed to check resources and emit warnings
 * @property tags special labels that can be used for splitting tests into groups
 * @property description free text with a description
 * @property suiteName name of test suite that can be visible from save-cloud
 * @property excludedTests excluded tests from the run
 * @property expectedWarningsPattern - pattern with warnings that are expected from the test file
 * @property runConfigPattern everything from the capture group will be split by comma and then by `=`
 */
@Serializable
data class GeneralConfig(
    val execCmd: String? = null,
    val tags: List<String>? = null,
    val description: String? = null,
    val suiteName: String? = null,
    val excludedTests: List<String>? = null,
    val expectedWarningsPattern: Regex? = null,
    val runConfigPattern: Regex? = null,
) : PluginConfig {
    override val type = TestConfigSections.GENERAL

    override var ignoreLinesPatterns: MutableList<Regex> = mutableListOf()

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as GeneralConfig
        val mergedTag = other.tags?.let {
            this.tags?.let {
                other.tags.union(this.tags)
            } ?: other.tags
        }?.toList() ?: this.tags

        return GeneralConfig(
            this.execCmd ?: other.execCmd,
            mergedTag,
            this.description ?: other.description,
            this.suiteName ?: other.suiteName,
            this.excludedTests ?: other.excludedTests,
            this.expectedWarningsPattern ?: other.expectedWarningsPattern,
            this.runConfigPattern ?: other.runConfigPattern
        ).also {
            it.configLocation = this.configLocation
        }
    }

    override fun validateAndSetDefaults(): GeneralConfig {
        requireNotNull(execCmd) {
            errorMsgForRequireCheck("execCmd")
        }
        requireNotNull(tags) {
            errorMsgForRequireCheck("tags")
        }
        requireNotNull(description) {
            errorMsgForRequireCheck("description")
        }
        requireNotNull(suiteName) {
            errorMsgForRequireCheck("suiteName")
        }
        return GeneralConfig(
            execCmd,
            tags,
            description,
            suiteName,
            excludedTests ?: emptyList(),
            expectedWarningsPattern ?: defaultExpectedWarningPattern,
            runConfigPattern ?: defaultRunConfigPattern,
        ).also {
            it.configLocation = this.configLocation
        }
    }

    private fun errorMsgForRequireCheck(field: String) =
            """
                        Error: Couldn't find `$field` in [general] section of `$configLocation` config.
                        Current configuration: ${this.toString().substringAfter("(").substringBefore(")")}
                        Please provide it in this, or at least in one of the parent configs.
            """.trimIndent()

    companion object {
        /**
         * Default regex for expected warnings in test resources, e.g.
         * `// ;warn:2:4: Class name in incorrect case`
         */
        val defaultExpectedWarningPattern = Regex("// ;warn:(.+):(\\d+): (.+)")
        val defaultRunConfigPattern = Regex("// RUN: (.+)")
    }
}

/**
 * @property args1 arguments to be inserted *before* file name
 * @property args2 arguments to be inserted *after* file name
 */
data class ExtraFlags(
    val args1: String,
    val args2: String,
) {
    companion object {
        const val KEY_ARGS_1 = "args1"
        const val KEY_ARGS_2 = "args2"
        val empty = ExtraFlags("", "")

        /**
         * Construct [ExtraFlags] from provided map
         *
         * @param map a map possibly containing values for [args1] and [args2], denoted by keys [KEY_ARGS_1] and [KEY_ARGS_2]
         * @return [ExtraFlags]
         */
        fun from(map: Map<String, String>) =
                ExtraFlags(map.getOrElse(KEY_ARGS_1) { "" }, map.getOrElse(KEY_ARGS_2) { "" })
    }
}

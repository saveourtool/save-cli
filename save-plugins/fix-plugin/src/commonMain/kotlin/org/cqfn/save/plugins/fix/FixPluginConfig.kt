@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.utils.RegexSerializer

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

/**
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execFlags a command that will be executed to mutate test file contents
 * @property batchSize it controls how many files execCmd will process at a time.
 * @property resourceNameTestSuffix suffix name of the test file.
 * @property resourceNameExpectedSuffix suffix name of the expected file.
 * @property batchSeparator
 * @property ignoreLines mutable list of patterns that later will be used to filter lines in test file
 */
@Serializable
data class FixPluginConfig(
    val execFlags: String? = null,
    val batchSize: Long? = null,
    val batchSeparator: String? = null,
    val resourceNameTestSuffix: String? = null,
    val resourceNameExpectedSuffix: String? = null,
    val ignoreLines: MutableList<String>? = null
) : PluginConfig {
    override val type = TestConfigSections.FIX

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()

    @Transient
    override val ignoreLinesPatterns: MutableList<Regex> = ignoreLines?.map { it.toRegex() }?.toMutableList() ?: mutableListOf()

    /**
     *  @property resourceNameTest
     */
    val resourceNameTest: String = resourceNameTestSuffix ?: "Test"

    /**
     *  @property resourceNameExpected
     */
    val resourceNameExpected: String = resourceNameExpectedSuffix ?: "Expected"
    override val resourceNamePatternStr: String = """(.+)($resourceNameExpected|$resourceNameTest)\.[\w\d]+"""

    /**
     *  @property resourceNamePattern regex for the name of the test files.
     */
    val resourceNamePattern: Regex = Regex(resourceNamePatternStr)

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixPluginConfig
        return FixPluginConfig(
            this.execFlags ?: other.execFlags,
            this.batchSize ?: other.batchSize,
            this.batchSeparator ?: other.batchSeparator,
            this.resourceNameTestSuffix ?: other.resourceNameTestSuffix,
            this.resourceNameExpectedSuffix ?: other.resourceNameExpectedSuffix,
            other.ignoreLines?.let {
                this.ignoreLines?.let { other.ignoreLines.union(this.ignoreLines) } ?: other.ignoreLines
            }?.toMutableList() ?: this.ignoreLines
        ).also {
            it.configLocation = this.configLocation
        }
    }

    // due to probable bug in ktoml, ignoreLines = [] and no ignoreLines is ktoml are parsed to be mutableListOf("null")
    override fun validateAndSetDefaults() = FixPluginConfig(
        execFlags ?: "",
        batchSize ?: 1,
        batchSeparator ?: ", ",
        resourceNameTest,
        resourceNameExpected,
        ignoreLines
    ).also {
        it.configLocation = this.configLocation
    }
}

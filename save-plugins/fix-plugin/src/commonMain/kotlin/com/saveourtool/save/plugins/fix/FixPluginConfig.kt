@file:UseSerializers(RegexSerializer::class)

package com.saveourtool.save.plugins.fix

import com.saveourtool.save.core.config.ActualFixFormat
import com.saveourtool.save.core.config.TestConfigSections
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.utils.RegexSerializer

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
 * @property execFlags a flags that will be applied to execCmd
 * @property resourceNameTestSuffix suffix name of the test file.
 * @property resourceNameExpectedSuffix suffix name of the expected file.
 * @property ignoreLines mutable list of patterns that later will be used to filter lines in test file
 * @property actualFixFormat format for type for fixes: they could be done in place or provided via Sarif file // TODO: Add information about these properties to README
 * @property actualFixSarifFileName name of sarif file with list of fixes, that were made by tool
 */
@Serializable
data class FixPluginConfig(
    val execFlags: String? = null,
    val resourceNameTestSuffix: String? = null,
    val resourceNameExpectedSuffix: String? = null,
    val ignoreLines: MutableList<String>? = null,
    val actualFixFormat: ActualFixFormat? = null,
    val actualFixSarifFileName: String? = null,
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
            this.resourceNameTestSuffix ?: other.resourceNameTestSuffix,
            this.resourceNameExpectedSuffix ?: other.resourceNameExpectedSuffix,
            other.ignoreLines?.let {
                this.ignoreLines?.let { other.ignoreLines.union(this.ignoreLines) } ?: other.ignoreLines
            }?.toMutableList() ?: this.ignoreLines,
            this.actualFixFormat ?: other.actualFixFormat,
            this.actualFixSarifFileName ?: other.actualFixSarifFileName
        ).also {
            it.configLocation = this.configLocation
        }
    }

    // due to probable bug in ktoml, ignoreLines = [] and no ignoreLines is ktoml are parsed to be mutableListOf("null")
    override fun validateAndSetDefaults(): FixPluginConfig = FixPluginConfig(
        execFlags = execFlags ?: "",
        resourceNameTestSuffix = resourceNameTest,
        resourceNameExpectedSuffix = resourceNameExpected,
        ignoreLines = ignoreLines,
        actualFixFormat = actualFixFormat ?: ActualFixFormat.IN_PLACE,
        actualFixSarifFileName = calculateActualFixSarifFilePath(),
    ).also {
        it.configLocation = this.configLocation
    }

    // we require from sarif file to be located at the same level as corresponding save.toml
    private fun calculateActualFixSarifFilePath(): String? = if (actualFixFormat == ActualFixFormat.SARIF) {
        (
                configLocation.parent!! /
                        (actualFixSarifFileName ?: "save-fixes.sarif").toPath()
        ).toString()
    } else {
        null
    }
}

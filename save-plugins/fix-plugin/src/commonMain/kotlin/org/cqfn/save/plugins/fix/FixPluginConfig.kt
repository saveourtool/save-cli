@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.plugin.RegexSerializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execCmd a command that will be executed to mutate test file contents
 * @property resourceNameTestSuffix suffix name of the test file.
 * @property resourceNameExpectedSuffix suffix name of the expected file.
 */
@Serializable
data class FixPluginConfig(
    val execCmd: String,
    val resourceNameTestSuffix: String? = null,
    val resourceNameExpectedSuffix: String? = null,
) : PluginConfig {
    override val type = TestConfigSections.FIX

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixPluginConfig
        return FixPluginConfig(
            this.execCmd,
            this.resourceNameTestSuffix ?: other.resourceNameTestSuffix,
            this.resourceNameExpectedSuffix ?: other.resourceNameExpectedSuffix,
        )
    }

    /**
     *  @property resourceNamePattern regex for the name of the test files.
     */
    val resourceNamePattern: Regex = resourceNamePattern()

    /**
     *  @property resourceNameTest
     */
    val resourceNameTest: String = resourceNameTestSuffix ?: "Test"

    /**
     *  @property resourceNameExpected
     */
    val resourceNameExpected: String = resourceNameExpectedSuffix ?: "Expected"

    private fun resourceNamePattern(): Regex {
        val isNotNullResourceNameTestSuffix = resourceNameTestSuffix != null
        val isNotNullResourceNameExpectedSuffix = resourceNameExpectedSuffix != null
        return when (isNotNullResourceNameExpectedSuffix to isNotNullResourceNameTestSuffix) {
            true to true -> Regex("""(.+)($resourceNameExpectedSuffix|$resourceNameExpectedSuffix)\.[\w\d]+""")
            true to false -> Regex("""(.+)($resourceNameExpectedSuffix|Test)\.[\w\d]+""")
            false to true -> Regex("""(.+)(Expected|$resourceNameExpectedSuffix)\.[\w\d]+""")
            else -> Regex("""(.+)(Expected|Test)\.[\w\d]+""")
        }
    }

}

@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.utils.RegexSerializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execFlags a command that will be executed to mutate test file contents
 * @property resourceNameTestSuffix suffix name of the test file.
 * @property resourceNameExpectedSuffix suffix name of the expected file.
 */
@Serializable
data class FixPluginConfig(
    val execFlags: String,
    val resourceNameTestSuffix: String? = null,
    val resourceNameExpectedSuffix: String? = null,
) : PluginConfig {
    override val type = TestConfigSections.FIX

    /**
     *  @property resourceNameTest
     */
    val resourceNameTest: String = resourceNameTestSuffix ?: "Test"

    /**
     *  @property resourceNameExpected
     */
    val resourceNameExpected: String = resourceNameExpectedSuffix ?: "Expected"

    override fun mergePluginConfig(parentConfig: FixPluginConfig) = FixPluginConfig(
        this.execFlags ?: parentConfig.execFlags,
        this.destinationFileSuffix ?: parentConfig.destinationFileSuffix
    )
    /**
     *  @property resourceNamePattern regex for the name of the test files.
     */
    val resourceNamePattern: Regex = Regex("""(.+)($resourceNameExpected|$resourceNameTest)\.[\w\d]+""")

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixPluginConfig
        return FixPluginConfig(
            this.execCmd,
            this.resourceNameTestSuffix ?: other.resourceNameTestSuffix,
            this.resourceNameExpectedSuffix ?: other.resourceNameExpectedSuffix,
        )
    }
}

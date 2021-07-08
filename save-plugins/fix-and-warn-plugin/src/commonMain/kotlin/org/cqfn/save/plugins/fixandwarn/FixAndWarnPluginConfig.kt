package org.cqfn.save.plugins.fixandwarn

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @property fixPluginConfig config for nested [fix] section
 * @property warnPluginConfig config for nested [warn] section
 */
@Serializable
data class FixAndWarnPluginConfig(
    val fixPluginConfig: FixPluginConfig,
    val warnPluginConfig: WarnPluginConfig
) : PluginConfig {
    override val type = TestConfigSections.FIX_AND_WARN

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixAndWarnPluginConfig
        val mergedFixPluginConfig = fixPluginConfig.mergeWith(other.fixPluginConfig)
        val mergedWarnPluginConfig = warnPluginConfig.mergeWith(other.warnPluginConfig)
        return FixAndWarnPluginConfig(
            mergedFixPluginConfig as FixPluginConfig,
            mergedWarnPluginConfig as WarnPluginConfig
        )
    }

    override fun validateAndSetDefaults(): PluginConfig {
        require(fixPluginConfig.resourceNameTest == warnPluginConfig.testName && fixPluginConfig.batchSize == warnPluginConfig.batchSize) {
            """
               Test files suffix names and batch sizes should be identical for [fix] and [warn] plugins.
               But found [fix]: {${fixPluginConfig.resourceNameTest}, ${fixPluginConfig.batchSize}},
                         [warn]: {${warnPluginConfig.testName}, ${warnPluginConfig.batchSize}}
           """
        }
        return FixAndWarnPluginConfig(
            fixPluginConfig.validateAndSetDefaults(),
            warnPluginConfig.validateAndSetDefaults()
        )
    }
}

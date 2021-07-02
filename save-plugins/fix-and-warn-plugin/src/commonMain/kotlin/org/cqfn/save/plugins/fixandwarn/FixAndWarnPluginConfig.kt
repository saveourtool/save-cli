package org.cqfn.save.plugins.fixandwarn

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

@Serializable
data class FixAndWarnPluginConfig(
    val fixPluginConfig: FixPluginConfig,
    val warnPluginConfig: WarnPluginConfig
): PluginConfig {
    override val type = TestConfigSections.FIX_AND_WARN

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()
    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixAndWarnPluginConfig
        val mergedFixPlugin = fixPluginConfig.mergeWith(other.fixPluginConfig)
        val mergedWarnPlugin = warnPluginConfig.mergeWith(other.warnPluginConfig)
        return FixAndWarnPluginConfig(
            mergedFixPlugin as FixPluginConfig,
            mergedWarnPlugin as WarnPluginConfig
        )
    }

    override fun validateAndSetDefaults(): PluginConfig {
        // TODO: test patterns should be the same in fix and warn plugins
        return FixAndWarnPluginConfig(
            fixPluginConfig.validateAndSetDefaults(),
            warnPluginConfig.validateAndSetDefaults()
        )
    }
}

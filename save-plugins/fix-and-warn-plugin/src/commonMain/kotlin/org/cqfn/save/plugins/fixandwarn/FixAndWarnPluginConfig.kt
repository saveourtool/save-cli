package org.cqfn.save.plugins.fixandwarn

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.utils.RegexSerializer
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

/**
 * @property fix config for nested [fix] section
 * @property warn config for nested [warn] section
 * @property inlineFixer
 * @property checkFixesPattern
 */
@Serializable
data class FixAndWarnPluginConfig(
    val fix: FixPluginConfig,
    val warn: WarnPluginConfig,
    val inlineFixer: Boolean? = null,
    val checkFixesPattern: Regex? = null,
) : PluginConfig {
    override val type = TestConfigSections.`FIX AND WARN`

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixAndWarnPluginConfig
        val mergedFixPluginConfig = fix.mergeWith(other.fix)
        val mergedWarnPluginConfig = warn.mergeWith(other.warn)
        return FixAndWarnPluginConfig(
            mergedFixPluginConfig as FixPluginConfig,
            mergedWarnPluginConfig as WarnPluginConfig,
            this.inlineFixer ?: otherConfig.inlineFixer,
            this.checkFixesPattern ?: other.checkFixesPattern,
        )
    }

    override fun validateAndSetDefaults(): PluginConfig {
        val fixPluginConfig = fix.validateAndSetDefaults()
        val warnPluginConfig = warn.validateAndSetDefaults()
        require(fixPluginConfig.resourceNameTest == warnPluginConfig.testNameSuffix &&
                fixPluginConfig.batchSize == warnPluginConfig.batchSize
        ) {
            """
               Test files suffix names and batch sizes should be identical for [fix] and [warn] plugins.
               But found [fix]: {${fixPluginConfig.resourceNameTest}, ${fixPluginConfig.batchSize}},
                         [warn]: {${warnPluginConfig.testNameSuffix}, ${warnPluginConfig.batchSize}}
           """
        }
        val newInlineFixer = inlineFixer ?: false
        val newCheckFixerPattern = if (newInlineFixer) (checkFixesPattern ?: defaultCheckFixesPattern) else null
        return FixAndWarnPluginConfig(
            fixPluginConfig,
            warnPluginConfig,
            newInlineFixer,
            newCheckFixerPattern,
        )
    }

    companion object {
        /**
         * Default regex for check fixes
         * ```CHECK-FIXES: val foo = 42```
         */
        internal val defaultCheckFixesPattern = Regex("(.+): (.+)")
    }
}

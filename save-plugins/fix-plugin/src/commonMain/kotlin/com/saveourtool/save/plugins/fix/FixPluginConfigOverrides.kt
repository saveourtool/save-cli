package com.saveourtool.save.plugins.fix

import com.saveourtool.save.core.config.TestConfigSections
import com.saveourtool.save.core.plugin.PluginConfigOverrides

data class FixPluginConfigOverrides(
    override val execCmd: String?,
    override val execFlags: String?,
) : PluginConfigOverrides {
    override val type: TestConfigSections
        get() = TestConfigSections.FIX

    data class Interim(
        val execCmd: String?,
        val execFlags: String?,
    ) : com.saveourtool.save.core.config.Interim<FixPluginConfigOverrides, Interim> {
        override fun merge(overrides: Interim) = Interim(
            overrides.execCmd ?: execCmd,
            overrides.execFlags ?: execFlags,
        )

        override fun build() = FixPluginConfigOverrides(
            execCmd,
            execFlags
        )
    }
}
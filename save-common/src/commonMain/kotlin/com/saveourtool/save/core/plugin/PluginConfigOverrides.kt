package com.saveourtool.save.core.plugin

import com.saveourtool.save.core.config.TestConfigSections
import okio.Path

interface PluginConfigOverrides {
    val type: TestConfigSections
    val execCmd: String?
    val execFlags: String?

    interface Factory<T : PluginConfigOverrides> {
        fun read(path: Path): T
    }
}

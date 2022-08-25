/**
 * This file contains util methods for [PluginConfig]
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.plugin.PluginConfig

/**
 * @return original list of configuration for plugins after validation and merged with default values
 */
fun MutableList<PluginConfig>.validateAndSetDefaults() {
    forEachIndexed { index, config ->
        this[index] = config.validateAndSetDefaults()
    }
}

/**
 * @param otherPluginConfigs list of configurations for plugins that are merged to current list
 * @return original list of configuration for plugins merged with [otherPluginConfigs]
 */
fun MutableList<PluginConfig>.mergeWith(otherPluginConfigs: List<PluginConfig>) {
    otherPluginConfigs.forEach { otherPluginConfig ->
        this.mergeOrOverride(otherPluginConfig)
    }
}

/**
 * @param otherPluginConfigs list of configurations for plugins that overrides current list
 * @return original list of configuration for plugins overridden by [otherPluginConfigs]
 */
fun MutableList<PluginConfig>.overrideBy(otherPluginConfigs: List<PluginConfig>) {
    otherPluginConfigs.forEach { otherPluginConfig ->
        this.mergeOrOverride(otherPluginConfig, merge = false)
    }
}

/**
 * @return a single [PluginConfig] with type [P] from current list
 */
inline fun <reified P : PluginConfig> List<PluginConfig>.singleIsInstance(): P = requireNotNull(this.singleIsInstanceOrNull()) {
    "Not found an element with type ${P::class}"
}

/**
 * @return a single [PluginConfig] with type [P] from current list or null
 */
inline fun <reified P : PluginConfig> List<PluginConfig>.singleIsInstanceOrNull(): P? = this.filterIsInstance<P>().singleOrNull()

private fun MutableList<PluginConfig>.mergeOrOverride(otherPluginConfig: PluginConfig, merge: Boolean = true) {
    val childConfigsWithIndex = this.withIndex().filter { (_, value) -> value.type == otherPluginConfig.type }
    if (childConfigsWithIndex.isEmpty()) {
        // if we haven't found a plugin from parent in a current list of plugins - we will simply copy it
        this.add(otherPluginConfig)
    } else {
        require(childConfigsWithIndex.size == 1) {
            "Duplicate config with type ${otherPluginConfig.type} in $this"
        }
        val (childIndex, childConfig) = childConfigsWithIndex.single()
        // else, we will merge plugin with a corresponding plugin from a parent config
        // we expect that there is only one plugin of such type, otherwise we will throw an exception

        this[childIndex] = if (merge) {
            logTrace("Merging process of ${otherPluginConfig.type} from $otherPluginConfig into $childConfig")
            childConfig.mergeWith(otherPluginConfig)
        } else {
            logTrace("Overriding process of ${otherPluginConfig.type} from $otherPluginConfig into $childConfig")
            otherPluginConfig.mergeWith(childConfig)
        }
    }
}

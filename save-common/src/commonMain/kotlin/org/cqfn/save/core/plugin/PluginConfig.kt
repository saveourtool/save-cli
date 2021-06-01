/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Core interface for plugin configuration
 */
interface PluginConfig<T : PluginConfig<T>> {
    /**
     * Merge current config into [childConfig]
     *
     * @param childConfig list of child configs, which will be filtered for merging
     */
    @Suppress("TYPE_ALIAS")
    fun mergeConfigInto(childConfig: MutableList<PluginConfig<*>>)

    /**
     * Function which is capable for merging inherited configurations
     *
     * @param parentConfig config which will be merged with [this]
     * @return corresponding merged config
     */
    fun mergePluginConfig(parentConfig: T): T
}

/**
 * General configuration for test suite.
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property tags FixMe: after ktoml will support lists we should change it
 * @property description
 * @property suiteName
 * @property excludedTests FixMe: after ktoml will support lists we should change it
 * @property includedTests FixMe: after ktoml will support lists we should change it
 * @property ignoreSaveComments if true then ignore warning comments
 */
@Serializable
data class GeneralConfig(
    val tags: String,
    val description: String,
    val suiteName: String,
    val excludedTests: String? = null,
    val includedTests: String? = null,
    val ignoreSaveComments: Boolean? = null
) : PluginConfig<GeneralConfig> {
    @Suppress("TYPE_ALIAS")
    override fun mergeConfigInto(childConfig: MutableList<PluginConfig<*>>) {
        val childGeneralConfig = childConfig.filterIsInstance<GeneralConfig>().firstOrNull()
        val newChildGeneralConfig = childGeneralConfig?.mergePluginConfig(this) ?: this
        // Now we update child config in place
        childGeneralConfig?.let {
            childConfig.set(childConfig.indexOf(childGeneralConfig), newChildGeneralConfig)
        } ?: childConfig.add(newChildGeneralConfig)
    }

    override fun mergePluginConfig(parentConfig: GeneralConfig): GeneralConfig {
        val mergedTag = parentConfig.tags?.let {
            val parentTags = parentConfig.tags.split(", ")
            val childTags = this.tags.split(", ")
            parentTags.union(childTags).joinToString(", ")
        } ?: this.tags
        return GeneralConfig(
            mergedTag,
            this.description ?: parentConfig.description,
            this.suiteName ?: parentConfig.suiteName,
            this.excludedTests ?: parentConfig.excludedTests,
            this.includedTests ?: parentConfig.includedTests,
            this.ignoreSaveComments ?: parentConfig.ignoreSaveComments
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Regex::class)
object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("regex", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }

    override fun deserialize(decoder: Decoder): Regex = Regex(decoder.decodeString())
}

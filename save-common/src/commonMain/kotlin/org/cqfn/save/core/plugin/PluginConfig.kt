/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import org.cqfn.save.core.config.TestConfigSections

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Core interface for plugin configuration (like warnPlugin/fixPluin/e.t.c)
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
interface PluginConfig {
    /**
     * type of the config (usually related to the class: WARN/FIX/e.t.c)
     */
    val type: TestConfigSections

    /**
     * @param otherConfig - 'this' will be merged with 'other'
     * @return merged config
     */
    fun mergeWith(otherConfig: PluginConfig): PluginConfig
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
) : PluginConfig {
    override val type = TestConfigSections.GENERAL

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as GeneralConfig
        val mergedTag = other.tags?.let {
            this.tags?.let {
                val parentTags = other.tags.split(", ")
                val childTags = this.tags.split(", ")
                parentTags.union(childTags).joinToString(", ")
            } ?: other.tags
        } ?: this.tags

        return GeneralConfig(
            mergedTag,
            this.description,
            this.suiteName,
            this.excludedTests ?: other.excludedTests,
            this.includedTests ?: other.includedTests,
            this.ignoreSaveComments ?: other.ignoreSaveComments
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

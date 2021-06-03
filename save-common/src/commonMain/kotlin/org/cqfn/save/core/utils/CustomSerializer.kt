@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MatchingDeclarationName")

package org.cqfn.save.core.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

/**
 * Custom serializers that can be useful for reporting
 */

package org.cqfn.save.reporter.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Throwable::class)
object ThrowableSerializer : KSerializer<Throwable> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("throwable") {
        element("message", PrimitiveSerialDescriptor("message", PrimitiveKind.STRING), isOptional = true)
        // element("cause",)
    }

    override fun serialize(encoder: Encoder, value: Throwable) {
        encoder.encodeString(value.message!!)
    }

    override fun deserialize(decoder: Decoder): Throwable = Throwable(decoder.decodeString())
}

@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MatchingDeclarationName")

package com.saveourtool.save.core.utils

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("regex", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }

    override fun deserialize(decoder: Decoder): Regex = Regex(decoder.decodeString())
}

object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path = decoder.decodeString().toPath()
}

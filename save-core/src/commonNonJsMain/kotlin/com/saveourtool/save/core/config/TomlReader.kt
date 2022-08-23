package com.saveourtool.save.core.config

import com.akuleshov7.ktoml.Toml
import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.files.readFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromMap
import kotlinx.serialization.serializer
import okio.Path

@OptIn(ExperimentalSerializationApi::class)
class TomlReader {
    inline fun <reified T> read(filePath: Path): T {
        fs.read(filePath) {
            val serializer = Toml.serializersModule.serializer<T>()
            serializer.descriptor.
            val map: Map<String, *> = Toml.decodeFromString(this.readUtf8())
            return Properties.decodeFromMap(map as Map<String, Any>)
        }
    }

    private fun <T> customDeserizer(kSerializer: KSerializer<T>): KSerializer<T> {
        return object : KSerializer<T> {
            override val descriptor: SerialDescriptor
                get() = kSerializer.descriptor

            override fun deserialize(decoder: Decoder): T {
                TODO("Not yet implemented")
                decoder.beginStructure()
            }

            override fun serialize(encoder: Encoder, value: T) {
                TODO("Not yet implemented")
            }

        }
    }
}
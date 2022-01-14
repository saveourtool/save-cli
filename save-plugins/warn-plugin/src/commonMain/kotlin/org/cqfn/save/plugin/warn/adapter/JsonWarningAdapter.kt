package org.cqfn.save.plugin.warn.adapter

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cqfn.save.plugin.warn.utils.Warning

abstract class KxSerializationWarningAdapter<T>(
    private val stringFormat: StringFormat,
    private val deserializationStrategy: DeserializationStrategy<T>,
) : WarningAdapter<T> {
    fun toWarnings(rawReport: String, ctx: AdapterContext): List<Warning> {
        return toWarnings(
            stringFormat.decodeFromString(deserializationStrategy, rawReport),
            ctx,
        )
    }
}

inline fun <reified T> WarningAdapter<T>.jsonStringToWarnings(
    rawReport: String,
    ctx: AdapterContext,
): List<Warning> {
    return toWarnings(
        Json.decodeFromString(rawReport),
        ctx,
    )
}

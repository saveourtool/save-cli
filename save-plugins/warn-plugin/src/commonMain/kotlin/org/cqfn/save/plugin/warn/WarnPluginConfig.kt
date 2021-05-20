@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.plugin.PluginConfig

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @property execCmd a command that will be executed to check resources and emit warnings
 * @property warningsInputPattern a regular expression by which expected warnings will be discovered in test resources
 * @property warningsOutputPattern a regular expression by which warnings will be discovered in the process output
 * @property warningTextHasLine whether line number is included in [warningsOutputPattern]
 * @property warningTextHasColumn whether column number is included in [warningsOutputPattern]
 * @property lineCaptureGroup an index of capture group in regular expressions, corresponding to line number. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property columnCaptureGroup an index of capture group in regular expressions, corresponding to column number. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property messageCaptureGroup an index of capture group in regular expressions, corresponding to warning text. Indices start at 0 with 0
 * corresponding to the whole string.
 */
@Serializable
data class WarnPluginConfig(
    val execCmd: String,
    val warningsInputPattern: Regex,
    val warningsOutputPattern: Regex,
    val warningTextHasLine: Boolean = true,
    val warningTextHasColumn: Boolean = true,
    val lineCaptureGroup: Int?,
    val columnCaptureGroup: Int?,
    val messageCaptureGroup: Int,
) : PluginConfig {
    init {
        require(warningTextHasLine xor (lineCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasLine should be false (actual: $warningTextHasLine) " +
                    "or lineCaptureGroup should be provided (actual: $lineCaptureGroup}"
        }
        require(warningTextHasColumn xor (columnCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasColumn should be false (actual: $warningTextHasColumn) " +
                    "or columnCaptureGroup should be provided (actual: $columnCaptureGroup}"
        }
    }

    companion object {
        /**
         * Default regex for expected warnings in test resources, e.g.
         * `// ;warn:2:4: Class name in incorrect case`
         */
        internal val defaultInputPattern = Regex(";warn:(\\d+):(\\d+): (.+)")

        /**
         * Default regex for actual warnings in the tool output, e.g.
         * ```[WARN] /path/to/resources/ClassNameTest.java:2:4: Class name in incorrect case```
         */
        internal val defaultOutputPattern = Regex(".*(\\d+):(\\d+): (.+)")
        internal val defaultResourceNamePattern = Regex("""(.+)Test\.[\w\d]+""")
    }
}

@ExperimentalSerializationApi
@Serializer(forClass = Regex::class)
object RegexSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("regex", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }

    override fun deserialize(decoder: Decoder): Regex = Regex(decoder.decodeString())
}

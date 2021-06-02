@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfigSections
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
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
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
 * @property exactWarningsMatch exact match of errors
 */
@Serializable
data class WarnPluginConfig(
    val execCmd: String,
    val warningsInputPattern: Regex? = null,
    val warningsOutputPattern: Regex? = null,
    val warningTextHasLine: Boolean? = null,
    val warningTextHasColumn: Boolean? = null,
    val lineCaptureGroup: Int? = null,
    val columnCaptureGroup: Int? = null,
    val messageCaptureGroup: Int,
    val exactWarningsMatch: Boolean? = null,
) : PluginConfig {
    override val type = TestConfigSections.WARN

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as WarnPluginConfig
        return WarnPluginConfig(
            this.execCmd,
            this.warningsInputPattern,
            this.warningsOutputPattern,
            this.warningTextHasLine ?: other.warningTextHasLine,
            this.warningTextHasColumn ?: other.warningTextHasColumn,
            this.lineCaptureGroup ?: other.lineCaptureGroup,
            this.columnCaptureGroup ?: other.columnCaptureGroup,
            this.messageCaptureGroup,
            this.exactWarningsMatch ?: other.exactWarningsMatch
        )
    }

    override fun validateAndSetDefaults(): WarnPluginConfig {
        require((warningTextHasLine == true) xor (lineCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasLine should be false (actual: $warningTextHasLine) " +
                    "or lineCaptureGroup should be provided (actual: $lineCaptureGroup)"
        }
        require((warningTextHasColumn == true) xor (columnCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasColumn should be false (actual: $warningTextHasColumn) " +
                    "or columnCaptureGroup should be provided (actual: $columnCaptureGroup)"
        }

        val newWarningsInputPattern = warningsInputPattern ?: defaultInputPattern
        val newWarningsOutputPattern = warningsOutputPattern ?: defaultOutputPattern
        val newWarningTextHasLine = warningTextHasLine ?: false
        val newWarningTextHasColumn = warningTextHasColumn ?: false
        val newExactWarningsMatch = exactWarningsMatch ?: true
        return WarnPluginConfig(
            execCmd,
            newWarningsInputPattern,
            newWarningsOutputPattern,
            newWarningTextHasLine,
            newWarningTextHasColumn,
            lineCaptureGroup,
            columnCaptureGroup,
            messageCaptureGroup,
            newExactWarningsMatch
        )
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

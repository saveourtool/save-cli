@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.utils.RegexSerializer

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

/**
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execFlags a command that will be executed to check resources and emit warnings
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
 * @property fileNameCaptureGroupOut an index of capture group in regular expressions, corresponding to file name. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property lineCaptureGroupOut an index of capture group in regular expressions, corresponding to line number. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property columnCaptureGroupOut an index of capture group in regular expressions, corresponding to column number. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property messageCaptureGroupOut an index of capture group in regular expressions, corresponding to warning text. Indices start at 0 with 0
 * corresponding to the whole string.
 * @property exactWarningsMatch exact match of errors
 * @property testNameSuffix suffix name of the test file.
 * @property batchSize
 */
@Serializable
data class WarnPluginConfig(
    val execFlags: String? = null,
    val warningsInputPattern: Regex? = null,
    val warningsOutputPattern: Regex? = null,
    val warningTextHasLine: Boolean? = null,
    val warningTextHasColumn: Boolean? = null,
    val batchSize: Int? = null,
    val lineCaptureGroup: Int? = null,
    val columnCaptureGroup: Int? = null,
    val messageCaptureGroup: Int? = null,
    val fileNameCaptureGroupOut: Int? = null,
    val lineCaptureGroupOut: Int? = null,
    val columnCaptureGroupOut: Int? = null,
    val messageCaptureGroupOut: Int? = null,
    val exactWarningsMatch: Boolean? = null,
    val testNameSuffix: String? = null,
) : PluginConfig {
    override val type = TestConfigSections.WARN

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()
    private val testName: String = testNameSuffix ?: "Test"

    /**
     *  @property resourceNamePattern regex for the name of the test files.
     */
    val resourceNamePattern: Regex = Regex("""(.+)${(testName)}\.[\w\d]+""")

    @Suppress("ComplexMethod")
    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as WarnPluginConfig
        return WarnPluginConfig(
            this.execFlags ?: other.execFlags,
            this.warningsInputPattern ?: other.warningsInputPattern,
            this.warningsOutputPattern ?: other.warningsOutputPattern,
            this.warningTextHasLine ?: other.warningTextHasLine,
            this.warningTextHasColumn ?: other.warningTextHasColumn,
            this.batchSize ?: other.batchSize,
            this.lineCaptureGroup ?: other.lineCaptureGroup,
            this.columnCaptureGroup ?: other.columnCaptureGroup,
            this.messageCaptureGroup ?: other.messageCaptureGroup,
            this.fileNameCaptureGroupOut ?: other.fileNameCaptureGroupOut,
            this.lineCaptureGroupOut ?: other.lineCaptureGroupOut,
            this.columnCaptureGroupOut ?: other.columnCaptureGroupOut,
            this.messageCaptureGroupOut ?: other.messageCaptureGroupOut,
            this.exactWarningsMatch ?: other.exactWarningsMatch,
            this.testNameSuffix ?: other.testNameSuffix
        )
    }

    @Suppress(
        "MAGIC_NUMBER",
        "MagicNumber",
        "ComplexMethod")
    override fun validateAndSetDefaults(): WarnPluginConfig {
        val newWarningTextHasLine = warningTextHasLine ?: true
        val newWarningTextHasColumn = warningTextHasColumn ?: true
        val newBatchSize = batchSize ?: 1
        val newLineCaptureGroup = if (newWarningTextHasLine) (lineCaptureGroup ?: 1) else null
        val newColumnCaptureGroup = if (newWarningTextHasColumn) (columnCaptureGroup ?: 2) else null
        val newMessageCaptureGroup = messageCaptureGroup ?: 3
        val newFileNameCaptureGroupOut = fileNameCaptureGroupOut ?: 1
        val newLineCaptureGroupOut = if (newWarningTextHasLine) (lineCaptureGroupOut ?: 2) else null
        val newColumnCaptureGroupOut = if (newWarningTextHasColumn) (columnCaptureGroupOut ?: 3) else null
        val newMessageCaptureGroupOut = messageCaptureGroupOut ?: 4
        requirePositiveIfNotNull(lineCaptureGroup)
        requirePositiveIfNotNull(columnCaptureGroup)
        requirePositiveIfNotNull(messageCaptureGroup)
        return WarnPluginConfig(
            execFlags ?: "",
            warningsInputPattern ?: defaultInputPattern,
            warningsOutputPattern ?: defaultOutputPattern,
            newWarningTextHasLine,
            newWarningTextHasColumn,
            newBatchSize,
            newLineCaptureGroup,
            newColumnCaptureGroup,
            newMessageCaptureGroup,
            newFileNameCaptureGroupOut,
            newLineCaptureGroupOut,
            newColumnCaptureGroupOut,
            newMessageCaptureGroupOut,
            exactWarningsMatch ?: true,
            testName
        )
    }

    private fun requirePositiveIfNotNull(value: Int?) {
        value?.let {
            require(value >= 0) {
                """
                    Error: All integer values in [warn] section of `$configLocation` config should be positive!
                    Current configuration: ${this.toString().substringAfter("(").substringBefore(")")}
                """.trimIndent()
            }
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
        internal val defaultOutputPattern = Regex("(.+):(\\d+):(\\d+): (.+)")
    }
}

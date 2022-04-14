@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.ActualWarningsFormat
import org.cqfn.save.core.config.ExpectedWarningsFormat
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
 * @property actualWarningsPattern a regular expression by which warnings will be discovered in the process output
 * @property warningTextHasLine whether line number is included in [actualWarningsPattern]
 * @property warningTextHasColumn whether column number is included in [actualWarningsPattern]
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
 * @property testNameRegex regular expression, which defines a test-file's name.
 * @property batchSize it controls how many files execCmd will process at a time.
 * @property batchSeparator separator for batch mode
 * @property linePlaceholder placeholder for line number, which resolved as current line and support addition and subtraction
 * @property wildCardInDirectoryMode mode that controls that we are targeting our tested tools on directories (not on files)
 * This prefix will be added to the name of the directory, if you would like to use directory mode without any prefix simply use ""
 * @property patternForRegexInWarning symbols that will be used to detect regular expressions in the text of expected warnings in test resource file.
 * For example: for `[warn] my {{[hello|world]}} warn` patternForRegexInWarning = {{.*}}. Opening and closing symbols should be split with '.*' symbol.
 * @property partialWarnTextMatch if true - the regex created from expected warning will be wrapped with '.*': .*warn.*.
 * That can help a user to write only main information in the warning without any need to add/copy-paste technical info
 * @property testToolResFileOutput file with actual warnings
 * @property ignoreLines mutable list of patterns that later will be ignored in test files
 * @property benchmarkMode whether to ignore the warning messages
 * @property messageCaptureGroupMiddle
 * @property messageCaptureGroupEnd
 * @property expectedWarningsFormat
 * @property actualWarningsFormat
 * @property expectedWarningsFileName
 * @property actualWarningsFileName
 */
@Serializable
data class WarnPluginConfig(
    val execFlags: String? = null,
    val actualWarningsPattern: Regex? = null,
    val warningTextHasLine: Boolean? = null,
    val warningTextHasColumn: Boolean? = null,
    val batchSize: Long? = null,
    val batchSeparator: String? = null,
    val lineCaptureGroup: Long? = null,
    val columnCaptureGroup: Long? = null,
    val messageCaptureGroup: Long? = null,
    val messageCaptureGroupMiddle: Long? = null,
    val messageCaptureGroupEnd: Long? = null,
    val fileNameCaptureGroupOut: Long? = null,
    val lineCaptureGroupOut: Long? = null,
    val columnCaptureGroupOut: Long? = null,
    val messageCaptureGroupOut: Long? = null,
    val exactWarningsMatch: Boolean? = null,
    val testNameRegex: String? = null,
    val linePlaceholder: String? = null,
    val wildCardInDirectoryMode: String? = null,
    val patternForRegexInWarning: List<String>? = null,
    val partialWarnTextMatch: Boolean? = null,
    val testToolResFileOutput: String? = null,
    val ignoreLines: MutableList<String>? = null,
    val benchmarkMode: Boolean? = null,
    val expectedWarningsFormat: ExpectedWarningsFormat? = null,
    val actualWarningsFormat: ActualWarningsFormat? = null,
    val expectedWarningsFileName: String? = null,
    val actualWarningsFileName: String? = null,
) : PluginConfig {
    @Transient
    override val type = TestConfigSections.WARN

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()

    @Transient
    override val ignoreLinesPatterns: MutableList<Regex> = ignoreLines?.map { it.toRegex() }?.toMutableList() ?: mutableListOf()

    /**
     * regex for name of the test file.
     */
    override val resourceNamePatternStr: String = testNameRegex ?: ".*Test.*"

    /**
     * regex for the name of the test files.
     */
    val resourceNamePattern: Regex = Regex(resourceNamePatternStr)

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as WarnPluginConfig
        return WarnPluginConfig(
            this.execFlags ?: other.execFlags,
            this.actualWarningsPattern ?: other.actualWarningsPattern,
            this.warningTextHasLine ?: other.warningTextHasLine,
            this.warningTextHasColumn ?: other.warningTextHasColumn,
            this.batchSize ?: other.batchSize,
            this.batchSeparator ?: other.batchSeparator,
            this.lineCaptureGroup ?: other.lineCaptureGroup,
            this.columnCaptureGroup ?: other.columnCaptureGroup,
            this.messageCaptureGroup ?: other.messageCaptureGroup,
            this.messageCaptureGroupMiddle ?: other.messageCaptureGroupMiddle,
            this.messageCaptureGroupEnd ?: other.messageCaptureGroupEnd,
            this.fileNameCaptureGroupOut ?: other.fileNameCaptureGroupOut,
            this.lineCaptureGroupOut ?: other.lineCaptureGroupOut,
            this.columnCaptureGroupOut ?: other.columnCaptureGroupOut,
            this.messageCaptureGroupOut ?: other.messageCaptureGroupOut,
            this.exactWarningsMatch ?: other.exactWarningsMatch,
            this.testNameRegex ?: other.testNameRegex,
            this.linePlaceholder ?: other.linePlaceholder,
            this.wildCardInDirectoryMode ?: other.wildCardInDirectoryMode,
            this.patternForRegexInWarning ?: other.patternForRegexInWarning,
            this.partialWarnTextMatch ?: other.partialWarnTextMatch,
            this.testToolResFileOutput ?: other.testToolResFileOutput,
            other.ignoreLines?.let {
                this.ignoreLines?.let { other.ignoreLines.union(this.ignoreLines) } ?: other.ignoreLines
            }?.toMutableList() ?: this.ignoreLines,
            this.benchmarkMode ?: other.benchmarkMode,
            expectedWarningsFormat = expectedWarningsFormat ?: other.expectedWarningsFormat,
            actualWarningsFormat = actualWarningsFormat ?: other.actualWarningsFormat,
            expectedWarningsFileName = expectedWarningsFileName ?: other.expectedWarningsFileName,
            actualWarningsFileName = actualWarningsFileName ?: other.actualWarningsFileName,
        ).also {
            it.configLocation = this.configLocation
        }
    }

    @Suppress(
        "MAGIC_NUMBER",
        "MagicNumber",
        "ComplexMethod",
        "TOO_LONG_FUNCTION"
    )
    override fun validateAndSetDefaults(): WarnPluginConfig {
        requirePositiveIfNotNull(lineCaptureGroup)
        requirePositiveIfNotNull(columnCaptureGroup)
        requirePositiveIfNotNull(messageCaptureGroup)
        requirePositiveIfNotNull(fileNameCaptureGroupOut)
        requirePositiveIfNotNull(lineCaptureGroupOut)
        requirePositiveIfNotNull(columnCaptureGroupOut)
        requirePositiveIfNotNull(messageCaptureGroupOut)
        requirePositiveIfNotNull(batchSize)
        requireValidPatternForRegexInWarning()

        val expectedWarningsFormat = expectedWarningsFormat ?: ExpectedWarningsFormat.IN_PLACE
        val actualWarningsFormat = actualWarningsFormat ?: ActualWarningsFormat.PLAIN
        val expectedWarningsFileName = expectedWarningsFileName ?: "save-warnings.sarif"

        val newWarningTextHasLine = warningTextHasLine ?: true
        val newWarningTextHasColumn = warningTextHasColumn ?: true

        val newLineCaptureGroup = if (newWarningTextHasLine) {
            (lineCaptureGroup ?: 1)
        } else {
            null
        }
        val newColumnCaptureGroup = if (newWarningTextHasColumn) (columnCaptureGroup ?: 2) else null
        val newMessageCaptureGroup = messageCaptureGroup ?: 3
        val newFileNameCaptureGroupOut = fileNameCaptureGroupOut ?: 1
        val newLineCaptureGroupOut = if (newWarningTextHasLine) (lineCaptureGroupOut ?: 2) else null
        val newColumnCaptureGroupOut = if (newWarningTextHasColumn) (columnCaptureGroupOut ?: 3) else null
        val newMessageCaptureGroupOut = messageCaptureGroupOut ?: 4

        return WarnPluginConfig(
            execFlags ?: "",
            actualWarningsPattern ?: defaultOutputPattern,
            newWarningTextHasLine,
            newWarningTextHasColumn,
            batchSize ?: 1,
            batchSeparator ?: ", ",
            newLineCaptureGroup,
            newColumnCaptureGroup,
            newMessageCaptureGroup,
            messageCaptureGroupMiddle ?: 1,
            messageCaptureGroupEnd ?: 1,
            newFileNameCaptureGroupOut,
            newLineCaptureGroupOut,
            newColumnCaptureGroupOut,
            newMessageCaptureGroupOut,
            exactWarningsMatch ?: true,
            resourceNamePatternStr,
            linePlaceholder ?: "\$line",
            wildCardInDirectoryMode,
            patternForRegexInWarning ?: defaultPatternForRegexInWarning,
            partialWarnTextMatch ?: false,
            testToolResFileOutput,
            ignoreLines,
            benchmarkMode ?: false,
            expectedWarningsFormat = expectedWarningsFormat,
            actualWarningsFormat = actualWarningsFormat,
            expectedWarningsFileName = expectedWarningsFileName,
            actualWarningsFileName = actualWarningsFileName,
        ).also {
            it.configLocation = this.configLocation
        }
    }

    private fun requirePositiveIfNotNull(value: Long?) {
        value?.let {
            require(value >= 0) {
                """
                    [Configuration Error]: All integer values in [warn] section of `$configLocation` config should be positive!
                    Current configuration: ${this.toString().substringAfter("(").substringBefore(")")}
                """.trimIndent()
            }
        }
    }

    private fun requireValidPatternForRegexInWarning() {
        patternForRegexInWarning?.let {
            require(patternForRegexInWarning.size == 2) {
                """
                    [Configuration Error]: Invalid pattern was provided for the configuration 'patternForRegexInWarning' in [warn] section of `$configLocation` config.
                    Opening and closing symbols that you expect to use as delimiters of regex should be placed in array and split with a comma.
                    For example, for: "[warn] my {{[hello|world]}} warn" patternForRegexInWarning is ["{{", "}}"].
                """.trimIndent()
            }
        }
    }

    companion object {
        /**
         * Default regex for actual warnings in the tool output, e.g.
         * ```[WARN] /path/to/resources/ClassNameTest.java:2:4: Class name in incorrect case```
         */
        internal val defaultOutputPattern = Regex("(.+):(\\d*):(\\d*): (.+)")
        internal val defaultPatternForRegexInWarning = listOf("{{", "}}")
    }
}

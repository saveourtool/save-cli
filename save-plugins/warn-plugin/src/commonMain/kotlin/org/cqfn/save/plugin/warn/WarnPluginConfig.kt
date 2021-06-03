@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:UseSerializers(RegexSerializer::class)

package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.utils.RegexSerializer


import kotlinx.serialization.Serializable

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
 * @property exactWarningsMatch exact match of errors
 * @property testNameSuffix suffix name of the test file.
 */
@Serializable
data class WarnPluginConfig(
    val execFlags: String? = null,
    val warningsInputPattern: Regex? = null,
    val warningsOutputPattern: Regex? = null,
    val warningTextHasLine: Boolean? = null,
    val warningTextHasColumn: Boolean? = null,
    val lineCaptureGroup: Int? = null,
    val columnCaptureGroup: Int? = null,
    val messageCaptureGroup: Int? = null,
    val exactWarningsMatch: Boolean? = null,
    val testNameSuffix: String? = null,
) : PluginConfig {
    override val type = TestConfigSections.WARN

    /**
     *  @property testName
     */
    val testName: String = testNameSuffix ?: "Test"

    /**
     *  @property resourceNamePattern regex for the name of the test files.
     */
    val resourceNamePattern: Regex = Regex("""(.+)${(testName)}\.[\w\d]+""")

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as WarnPluginConfig
        return WarnPluginConfig(
            this.execFlags ?: other.execFlags,
            this.warningsInputPattern ?: other.warningsInputPattern,
            this.warningsOutputPattern ?: other.warningsOutputPattern,
            this.warningTextHasLine ?: other.warningTextHasLine,
            this.warningTextHasColumn ?: other.warningTextHasColumn,
            this.lineCaptureGroup ?: other.lineCaptureGroup,
            this.columnCaptureGroup ?: other.columnCaptureGroup,
            this.messageCaptureGroup ?: other.columnCaptureGroup,
            this.exactWarningsMatch ?: other.exactWarningsMatch,
            this.testNameSuffix ?: other.testNameSuffix
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
        return WarnPluginConfig(
            execFlags,
            warningsInputPattern ?: defaultInputPattern,
            warningsOutputPattern ?: defaultOutputPattern,
            warningTextHasLine ?: true,
            warningTextHasColumn ?: true,
            lineCaptureGroup ?: 2,
            columnCaptureGroup ?: 3,
            messageCaptureGroup ?: 4,
            exactWarningsMatch ?: true,
            testName
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
    }
}

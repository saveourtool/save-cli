package com.saveourtool.save.core

import com.saveourtool.save.core.plugin.ExtraFlags
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.filterAndJoinBy
import com.saveourtool.save.core.plugin.resolvePlaceholdersFrom
import com.saveourtool.save.core.plugin.splitByNonEscaped

import okio.fakefilesystem.FakeFileSystem
import kotlin.js.JsName

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress(
    "TOO_LONG_FUNCTION",
    "WRONG_INDENTATION",  // issue in diktat
)
class ExtraFlagsExtractorTest {
    @Test
    @JsName("basicTest")
    fun `basic test`() {
        val extraFlagsExtractor = ExtraFlagsExtractor(
            GeneralConfig(runConfigPattern = Regex("""// RUN: (.*[^\\]=.*)""")),
            FakeFileSystem(),
        )

        listOf(
            "args1=stuff,args2=extraStuff" to ExtraFlags("stuff", "extraStuff"),
            "args1=stuff" to ExtraFlags("stuff", ""),
            "args2=extraStuff" to ExtraFlags("", "extraStuff"),
            "Unparseable nonsense" to ExtraFlags.empty,
            "args1=--flag --opt,args2=-debug --flag2" to ExtraFlags("--flag --opt", "-debug --flag2"),
            "args1=--flag\\=value,args2=--foo=bar" to ExtraFlags("--flag=value", "--foo=bar"),
            "args1=option1\\,option2,args2=option3\\,option4" to ExtraFlags("option1,option2", "option3,option4"),
            "args1=option1\\,option2" to ExtraFlags("option1,option2", ""),
            "args2=option3\\,option4" to ExtraFlags("", "option3,option4"),
        )
            .forEach { (line, extraFlags) ->
                assertEquals(extraFlags, extraFlagsExtractor.extractExtraFlagsFrom(line))
            }
    }

    @Test
    @JsName("shouldResolvePlaceholders")
    fun `should resolve placeholders`() {
        // basic test
        checkPlaceholders(
            "--foo --bar testFile --baz",
            "--foo \$args1 \$fileName \$args2",
            ExtraFlags("--bar", "--baz"),
            "testFile"
        )
        // only beforeFlags
        checkPlaceholders(
            "--foo --bar testFile",
            "--foo \$args1 \$fileName",
            ExtraFlags("--bar", ""),
            "testFile"
        )
        // only afterFlags
        checkPlaceholders(
            "--foo testFile --baz",
            "--foo \$fileName \$args2",
            ExtraFlags("", "--baz"),
            "testFile"
        )
        // only fileName
        checkPlaceholders(
            "--foo testFile",
            "--foo \$fileName",
            ExtraFlags("", ""),
            "testFile"
        )
        // no flags
        checkPlaceholders(
            "--foo testFile",
            "--foo",
            ExtraFlags("", ""),
            "testFile"
        )
    }

    @Test
    @JsName("shouldJoinMultilineDirectives")
    fun `should join multiline directives`() {
        checkMultilineDirectives(
            Regex("""// RUN: (.*([^\\]=)?.*)\\?"""),
            listOf(
                "// RUN: command --flag \\",
                "// RUN: --another-flag",
            ),
            listOf("command --flag --another-flag")
        )

        checkMultilineDirectives(
            Regex("""// RUN: (.*([^\\]=)?.*)\\?"""),
            listOf(
                "// RUN: command --flag \\",
                "// RUN: --another-flag \\",
                "// RUN: --yet-another-flag",
            ),
            listOf("command --flag --another-flag --yet-another-flag")
        )

        checkMultilineDirectives(
            Regex("""// RUN: (.*([^\\]=)?.*)\\?"""),
            listOf(
                "// RUN: command --flag \\",
                "// RUN: --another-flag",
                "// RUN: another-cmd \\",
                "// RUN: --flag",
            ),
            listOf(
                "command --flag --another-flag",
                "another-cmd --flag"
            )
        )

        checkMultilineDirectives(
            Regex("""// RUN: (.*([^\\]=)?.*)\\?"""),
            listOf(
                "// RUN: command --flag=option\\,\\",
                "// RUN: another-option --another-flag",
                "// RUN: another-cmd\\=\\",
                "// RUN: --flag=option\\,another-option",
            ),
            listOf(
                "command --flag=option\\,another-option --another-flag",
                "another-cmd\\=--flag=option\\,another-option"
            )
        )
    }

    @Test
    @JsName("testForSplitByNonEscaped")
    fun `test for splitByNonEscaped`() {
        assertEquals(
            listOf("this string\\, not split"),
            "this string\\, not split".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("this string", " but split"),
            "this string, but split".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("this string\\, not split", " but here - it's split"),
            "this string\\, not split, but here - it's split".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("", ""),
            ",".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("", "text"),
            ",text".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("\\,"),
            "\\,".splitByNonEscaped(','),
        )
        assertEquals(
            listOf("\\,text"),
            "\\,text".splitByNonEscaped(','),
        )
    }

    private fun checkMultilineDirectives(
        regex: Regex,
        lines: List<String>,
        expectedDirectives: List<String>,
    ) {
        assertEquals(
            expectedDirectives,
            lines.filterAndJoinBy(regex, '\\')
        )
    }

    private fun checkPlaceholders(
        execFlagsExpected: String,
        execFlagsFromConfig: String,
        extraFlags: ExtraFlags,
        fileName: String,
    ) {
        assertEquals(
            execFlagsExpected,
            resolvePlaceholdersFrom(execFlagsFromConfig, extraFlags, fileName)
        )
    }
}

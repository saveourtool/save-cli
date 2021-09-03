package org.cqfn.save.plugin.warn

import org.cqfn.save.plugin.warn.utils.ExtraFlagsExtractor

import okio.fakefilesystem.FakeFileSystem
import org.cqfn.save.plugin.warn.utils.resolvePlaceholdersFrom

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtraFlagsExtractorTest {
    @Test
    fun `basic test`() {
        val extraFlagsExtractor = ExtraFlagsExtractor(
            WarnPluginConfig(extraConfigPattern = Regex("""// RUN: (.*[^\\]=.*)""")),
            FakeFileSystem(),
        )

        listOf(
            "// RUN: beforeFlags=stuff,afterFlags=extraStuff" to ExtraFlags("stuff", "extraStuff"),
            "// RUN: beforeFlags=stuff" to ExtraFlags("stuff", ""),
            "// RUN: afterFlags=extraStuff" to ExtraFlags("", "extraStuff"),
            "// RUN: Unparseable nonsense" to null,
            "// RUN: beforeFlags=--flag --opt,afterFlags=-debug --flag2" to ExtraFlags("--flag --opt", "-debug --flag2"),
            "// RUN: beforeFlags=--flag\\=value,afterFlags=--foo=bar" to ExtraFlags("--flag=value", "--foo=bar"),
        )
            .forEach { (line, extraFlags) ->
                assertEquals(extraFlags, extraFlagsExtractor.extractExtraFlagsFrom(line))
            }
    }

    @Test
    fun `should resolve placeholders`() {
        // basic test
        checkPlaceholders(
            "--foo --bar testFile --baz",
            "--foo \$beforeFlags \$fileName \$afterFlags",
            ExtraFlags("--bar", "--baz"),
            "testFile"
        )
        // only beforeFlags
        checkPlaceholders(
            "--foo --bar testFile",
            "--foo \$beforeFlags \$fileName",
            ExtraFlags("--bar", ""),
            "testFile"
        )
        // only afterFlags
        checkPlaceholders(
            "--foo testFile --baz",
            "--foo \$fileName \$afterFlags",
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

    private fun checkPlaceholders(
        execFlagsExpected: String,
        execFlagsFromConfig: String,
        extraFlags: ExtraFlags,
        fileName: String,
    ) {
        assertEquals(
            execFlagsExpected,
            WarnPluginConfig(execFlags = execFlagsFromConfig)
                .resolvePlaceholdersFrom(extraFlags, fileName)
        )
    }
}

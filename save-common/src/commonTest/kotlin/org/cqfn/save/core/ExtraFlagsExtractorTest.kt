package org.cqfn.save.core

import org.cqfn.save.core.plugin.ExtraFlags
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.resolvePlaceholdersFrom

import okio.fakefilesystem.FakeFileSystem

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtraFlagsExtractorTest {
    @Test
    fun `basic test`() {
        val extraFlagsExtractor = ExtraFlagsExtractor(
            GeneralConfig(runConfigPattern = Regex("""// RUN: (.*[^\\]=.*)""")),
            FakeFileSystem(),
        )

        listOf(
            "// RUN: args1=stuff,args2=extraStuff" to ExtraFlags("stuff", "extraStuff"),
            "// RUN: args1=stuff" to ExtraFlags("stuff", ""),
            "// RUN: args2=extraStuff" to ExtraFlags("", "extraStuff"),
            "// RUN: Unparseable nonsense" to null,
            "// RUN: args1=--flag --opt,args2=-debug light --flag2" to ExtraFlags("--flag --opt", "-debug light --flag2"),
            "// RUN: args1=--flag\\=value,args2=--foo=bar" to ExtraFlags("--flag=value", "--foo=bar"),
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

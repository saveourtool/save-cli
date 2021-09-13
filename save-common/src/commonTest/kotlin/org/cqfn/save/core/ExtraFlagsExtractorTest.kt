package org.cqfn.save.core

import org.cqfn.save.core.plugin.ExtraFlags
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig

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
            "// RUN: args1=--flag --opt,args2=-debug --flag2" to ExtraFlags("--flag --opt", "-debug --flag2"),
            "// RUN: args1=--flag\\=value,args2=--foo=bar" to ExtraFlags("--flag=value", "--foo=bar"),
        )
            .forEach { (line, extraFlags) ->
                assertEquals(extraFlags, extraFlagsExtractor.extractExtraFlagsFrom(line))
            }
    }
}

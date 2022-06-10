package com.saveourtool.save.plugin.warn.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StringUtilsTest {
    @Test
    fun `checking the search of delimiters in the string`() {
        assertEquals("warn: {{hi}} {{hello}} world".findDelimitedSubStringsWith("{{", "}}"), mapOf(8 to 10, 15 to 20))
        assertEquals("warn: {{hi}} world".findDelimitedSubStringsWith("{{", "}}"), mapOf(8 to 10))
        assertEquals("warn: {{hi}}".findDelimitedSubStringsWith("{{", "}}"), mapOf(8 to 10))

        assertFailsWith<IllegalArgumentException> {
            "warn: {{hi}} }} {{hello}} world".findDelimitedSubStringsWith("{{", "}}")
        }

        assertFailsWith<IllegalArgumentException> {
            "warn: {{hi}} }} {{hello}} world }}".findDelimitedSubStringsWith("{{", "}}")
        }

        assertFailsWith<IllegalArgumentException> {
            "warn: {{hi hello".findDelimitedSubStringsWith("{{", "}}")
        }

        assertFailsWith<IllegalArgumentException> {
            "warn: }}hi hello".findDelimitedSubStringsWith("{{", "}}")
        }

        assertFailsWith<IllegalArgumentException> {
            "warn: hi hello{{".findDelimitedSubStringsWith("{{", "}}")
        }
    }

    @Test
    @Suppress("SAY_NO_TO_VAR")
    fun `checking the creation of regex messages from strings`() {
        var test = "my [special] string{{.*}}should be escaped"
        var regex = test.createRegexFromString("{{", "}}")
        var expected = "my [special] string that should be escaped"
        assertTrue { regex.matches(expected) }

        test = "{{(AAA|BBB)}} my [special] string should be escaped{{.*}}"
        regex = test.createRegexFromString("{{", "}}")
        expected = "AAA my [special] string should be escaped BBB"
        assertTrue { regex.matches(expected) }

        test = "{{.*}} A"
        regex = test.createRegexFromString("{{", "}}")
        expected = "AAA A"
        assertTrue { regex.matches(expected) }

        test = "B {{.*}}"
        regex = test.createRegexFromString("{{", "}}")
        expected = "B BBB"
        assertTrue { regex.matches(expected) }
    }

    @Test
    fun `regression with regular expressions`() {
        var test = "aaa{{ should }}bbb{{ UPPER_CASE }}ccc{{.*}}"
        var regex = test.createRegexFromString("{{", "}}")

        println(regex)

        test = "[ENUM_VALUE] enum values{{ should }}be in selected{{ UPPER_CASE }}snake/PascalCase format: PascAsl_f{{.*}}"
        regex = test.createRegexFromString("{{", "}}")

        println(regex)
    }
}

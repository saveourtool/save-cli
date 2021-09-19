package org.cqfn.save.plugin.warn.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringUtilsTest {
    @Test
    fun `checking the search of delimiters in the string`() {
        assertEquals("warn: {{hi}} {{hello}} world".findDelimitedSubStringsWith("{{", "}}"), mapOf(6 to 10, 13 to 20))
        assertEquals("warn: {{hi}} world".findDelimitedSubStringsWith("{{", "}}"), mapOf(6 to 10))
        assertEquals("warn: {{hi}}".findDelimitedSubStringsWith("{{", "}}"), mapOf(6 to 10))

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
}

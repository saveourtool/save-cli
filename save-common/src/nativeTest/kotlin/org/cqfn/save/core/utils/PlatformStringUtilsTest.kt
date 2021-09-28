package org.cqfn.save.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformStringUtilsTest {
    @Test
    fun `checking escaping of percents`() {
        assertEquals("%%", "%%".escapePercent())
        assertEquals("%%%%", "%%%".escapePercent())
        assertEquals("%%", "%%".escapePercent())
        assertEquals("%%aa%%", "%%aa%%".escapePercent())
        assertEquals("%%aa%%", "%aa%".escapePercent())
        assertEquals("a%%%%a", "a%%%%a".escapePercent())
        assertEquals("aaaaa", "aaaaa".escapePercent())
        assertEquals("a%%%%a", "a%%%a".escapePercent())
    }
}

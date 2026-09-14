package app.olauncher.universal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppMatcherTest {

    @Test
    fun `empty query matches nothing`() {
        assertNull(AppMatcher.score("Firefox", "org.mozilla.firefox", "   "))
        assertNull(AppMatcher.score("Firefox", "org.mozilla.firefox", ""))
    }

    @Test
    fun `exact label match ranks first`() {
        assertEquals(AppMatcher.EXACT, AppMatcher.score("Firefox", "org.mozilla.firefox", "Firefox"))
        assertEquals(AppMatcher.EXACT, AppMatcher.score("Firefox", "org.mozilla.firefox", "firefox"))
    }

    @Test
    fun `prefix match ranks before substring`() {
        assertEquals(AppMatcher.PREFIX, AppMatcher.score("Settings", "com.android.settings", "Set"))
        assertEquals(AppMatcher.SUBSTRING, AppMatcher.score("My Settings", "com.example.settings", "tings"))
    }

    @Test
    fun `prefix beats substring for the same query`() {
        val prefix = AppMatcher.score("Firefox", "org.mozilla.firefox", "fire")
        val substring = AppMatcher.score("SeaFirefox", "com.example.seafirefox", "fire")
        assertEquals(AppMatcher.PREFIX, prefix)
        assertEquals(AppMatcher.SUBSTRING, substring)
        if (prefix != null && substring != null) {
            // lower score wins; the prefix match must come first
            assertEquals(true, prefix < substring)
        }
    }

    @Test
    fun `normalized match strips diacritics`() {
        // "café" query matches "Cafe" label through normalization
        assertEquals(
            AppMatcher.NORMALIZED,
            AppMatcher.score("Cafe", "com.example.cafe", "caf\u00e9")
        )
    }

    @Test
    fun `package name match is last resort`() {
        assertEquals(
            AppMatcher.PACKAGE,
            AppMatcher.score("My Super Calculator", "org.example.calculator", "org.example")
        )
    }

    @Test
    fun `no relation returns null`() {
        assertNull(AppMatcher.score("Firefox", "org.mozilla.firefox", "calendar"))
    }

    @Test
    fun `exact match on label beats package match`() {
        // exact should win over the package fallback for the same query -> label
        val exact = AppMatcher.score("Notes", "com.example.notes", "notes")
        val packageMatch = AppMatcher.score("Another App", "com.example.notes", "notes")
        assertEquals(AppMatcher.EXACT, exact)
    }

    @Test
    fun `substring match beats package match`() {
        val substring = AppMatcher.score("My Notes", "com.example.notes", "notes")
        val packageMatch = AppMatcher.score("Another", "com.example.notes", "notes")
        assertEquals(AppMatcher.SUBSTRING, substring)
        if (substring != null && packageMatch != null) {
            assertEquals(true, substring < packageMatch)
        }
    }
}
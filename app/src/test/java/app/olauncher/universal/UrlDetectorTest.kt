package app.olauncher.universal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun `scheme urls are accepted as typed`() {
        assertEquals("https://github.com", UrlDetector.detect("https://github.com"))
        assertEquals("http://example.com", UrlDetector.detect("http://example.com"))
        assertEquals(
            "https://github.com/path?a=1#frag",
            UrlDetector.detect("https://github.com/path?a=1#frag")
        )
        assertEquals(
            "https://192.168.1.1",
            UrlDetector.detect("https://192.168.1.1")
        )
    }

    @Test
    fun `scheme-less urls with a dot are accepted`() {
        assertEquals("github.com", UrlDetector.detect("github.com"))
        assertEquals("www.example.com/test", UrlDetector.detect("www.example.com/test"))
        assertEquals("example.com:8080?q=1", UrlDetector.detect("example.com:8080?q=1"))
    }

    @Test
    fun `unicode idn hosts are accepted`() {
        assertEquals("unicode-doma\u00eene.example", UrlDetector.detect("unicode-doma\u00eene.example"))
    }

    @Test
    fun `sentences with whitespace are rejected`() {
        assertNull(UrlDetector.detect("hello world"))
        assertNull(UrlDetector.detect("https://example.com/a b"))
    }

    @Test
    fun `non web schemes are rejected`() {
        assertNull(UrlDetector.detect("ftp://example.com"))
        assertNull(UrlDetector.detect("mailto:user@example.com"))
        assertNull(UrlDetector.detect("javascript://example.com"))
    }

    @Test
    fun `bare words without a scheme become apps, not urls`() {
        assertNull(UrlDetector.detect("firefox"))
        assertNull(UrlDetector.detect("calendar"))
    }

    @Test
    fun `suspicious host labels are rejected`() {
        assertNull(UrlDetector.detect(".com"))
        assertNull(UrlDetector.detect("a..b"))
        assertNull(UrlDetector.detect("foo_bar.com"))
        assertNull(UrlDetector.detect("www."))
        assertNull(UrlDetector.detect("https://"))
        assertNull(UrlDetector.detect("http://"))
    }

    @Test
    fun `ip literals without a scheme are rejected`() {
        assertNull(UrlDetector.detect("192.168.1.1"))
        assertNull(UrlDetector.detect("192.168.1.1:8080"))
    }

    @Test
    fun `localhost is allowed as a scheme-less url`() {
        assertEquals("localhost", UrlDetector.detect("localhost"))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(UrlDetector.detect(""))
        assertNull(UrlDetector.detect("   "))
    }

    @Test
    fun `withScheme prepends https only when missing`() {
        assertEquals("https://github.com", UrlDetector.withScheme("github.com"))
        assertEquals("https://github.com", UrlDetector.withScheme("https://github.com"))
        assertEquals("http://example.com", UrlDetector.withScheme("http://example.com"))
    }
}
package com.aggreyshield.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun `valid email passes`() {
        assertNull(InputValidator.validateEmail("user@example.com"))
    }

    @Test
    fun `email without at sign fails`() {
        assertNotNull(InputValidator.validateEmail("userexample.com"))
    }

    @Test
    fun `blank email fails`() {
        assertNotNull(InputValidator.validateEmail("   "))
    }

    @Test
    fun `password shorter than minimum fails`() {
        assertNotNull(InputValidator.validateNewPassword("Ab1"))
    }

    @Test
    fun `password without uppercase fails`() {
        assertNotNull(InputValidator.validateNewPassword("lowercase123"))
    }

    @Test
    fun `password without digit fails`() {
        assertNotNull(InputValidator.validateNewPassword("NoDigitsHere"))
    }

    @Test
    fun `strong password passes`() {
        assertNull(InputValidator.validateNewPassword("StrongPass123"))
    }

    @Test
    fun `mismatched passwords fail`() {
        assertNotNull(InputValidator.validatePasswordMatch("Password123", "Password124"))
    }

    @Test
    fun `matching passwords pass`() {
        assertNull(InputValidator.validatePasswordMatch("Password123", "Password123"))
    }

    @Test
    fun `bare domain is accepted as a URL to scan`() {
        val result = InputValidator.checkUrl("example.com")
        assertTrue(result is UrlCheck.Valid)
        assertEquals("http://example.com", (result as UrlCheck.Valid).url)
    }

    @Test
    fun `https url is preserved as-is`() {
        val result = InputValidator.checkUrl("https://example.com/path")
        assertTrue(result is UrlCheck.Valid)
        assertEquals("https://example.com/path", (result as UrlCheck.Valid).url)
    }

    @Test
    fun `ftp scheme is rejected`() {
        val result = InputValidator.checkUrl("ftp://example.com")
        assertTrue(result is UrlCheck.Invalid)
    }

    @Test
    fun `url with a space is rejected`() {
        val result = InputValidator.checkUrl("exa mple.com")
        assertTrue(result is UrlCheck.Invalid)
    }

    @Test
    fun `blank url is rejected`() {
        val result = InputValidator.checkUrl("   ")
        assertTrue(result is UrlCheck.Invalid)
    }

    @Test
    fun `incident title below minimum length fails`() {
        assertNotNull(InputValidator.validateIncidentTitle("Bad"))
    }

    @Test
    fun `incident description below minimum length fails`() {
        assertNotNull(InputValidator.validateIncidentDescription("short"))
    }

    @Test
    fun `password strength increases with complexity`() {
        val weak = InputValidator.passwordStrength("abcdefghij")
        val strong = InputValidator.passwordStrength("Str0ng!Passw0rd#2026")
        assertTrue(strong.fraction > weak.fraction)
    }
}

package com.aggreyshield.app.core.network

import com.aggreyshield.app.core.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `401 with no body maps to Unauthorized`() {
        val error = ErrorMapper.fromHttp(401, null)
        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `403 maps to Forbidden`() {
        val error = ErrorMapper.fromHttp(403, null)
        assertTrue(error is AppError.Forbidden)
    }

    @Test
    fun `404 maps to NotFound`() {
        val error = ErrorMapper.fromHttp(404, null)
        assertTrue(error is AppError.NotFound)
    }

    @Test
    fun `429 with Retry-After maps to RateLimited with seconds parsed`() {
        val error = ErrorMapper.fromHttp(429, null, retryAfter = "30")
        assertTrue(error is AppError.RateLimited)
        assertEquals(30, (error as AppError.RateLimited).retryAfterSeconds)
    }

    @Test
    fun `500 maps to Server error with status code preserved`() {
        val error = ErrorMapper.fromHttp(500, null)
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).httpStatus)
    }

    @Test
    fun `503 maps to ServerUnavailable, not a generic Server error`() {
        val error = ErrorMapper.fromHttp(503, null)
        assertTrue(error is AppError.ServerUnavailable)
    }

    @Test
    fun `422 with field errors parses each field message`() {
        val body = """
            {"error":{"code":"VALIDATION_ERROR","message":"Request validation failed.",
            "details":[{"field":"body.email","message":"Enter a valid email address."}]}}
        """.trimIndent()
        val error = ErrorMapper.fromHttp(422, body)
        assertTrue(error is AppError.Validation)
        assertEquals("Enter a valid email address.", (error as AppError.Validation).fieldErrors["email"])
    }

    @Test
    fun `unrecognized json body does not crash and falls back to a safe message`() {
        val error = ErrorMapper.fromHttp(400, "not json at all")
        assertTrue(error is AppError.Client)
        assertEquals("The request could not be completed. Please check your input and try again.", error.userMessage)
    }

    @Test
    fun `server message longer than 200 chars is not surfaced verbatim`() {
        val longMessage = "x".repeat(500)
        val body = """{"error":{"code":"BAD","message":"$longMessage"}}"""
        val error = ErrorMapper.fromHttp(400, body)
        assertTrue(error.userMessage.length < 500)
    }
}

package com.aggreyshield.app.core.security

import com.aggreyshield.app.testutil.FakeTokenStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    @Test
    fun `restore with no stored refresh token is unauthenticated`() {
        val manager = SessionManager(FakeTokenStorage())
        manager.restore()
        assertEquals(SessionState.UNAUTHENTICATED, manager.state.value)
    }

    @Test
    fun `restore with a stored refresh token is authenticated`() {
        val storage = FakeTokenStorage()
        storage.saveTokens(AuthTokens(accessToken = "access-1", refreshToken = "refresh-1"))
        val manager = SessionManager(storage)
        manager.restore()
        assertEquals(SessionState.AUTHENTICATED, manager.state.value)
    }

    @Test
    fun `startSession stores tokens and becomes authenticated`() {
        val manager = SessionManager(FakeTokenStorage())
        manager.startSession("access-1", "refresh-1")
        assertEquals(SessionState.AUTHENTICATED, manager.state.value)
        assertEquals("access-1", manager.accessToken())
        assertEquals("refresh-1", manager.refreshToken())
    }

    @Test
    fun `voluntary logout clears tokens and does not raise the expired notice`() {
        val manager = SessionManager(FakeTokenStorage())
        manager.startSession("access-1", "refresh-1")
        manager.endSession(expired = false)
        assertEquals(SessionState.UNAUTHENTICATED, manager.state.value)
        assertNull(manager.accessToken())
        assertFalse(manager.consumeExpiredNotice())
    }

    @Test
    fun `forced session end raises the expired notice exactly once`() {
        val manager = SessionManager(FakeTokenStorage())
        manager.startSession("access-1", "refresh-1")
        manager.endSession(expired = true)
        assertEquals(SessionState.UNAUTHENTICATED, manager.state.value)
        assertTrue(manager.consumeExpiredNotice())
        assertFalse(manager.consumeExpiredNotice())
    }

    @Test
    fun `updateTokens rotates the access token without changing session state`() {
        val manager = SessionManager(FakeTokenStorage())
        manager.startSession("access-1", "refresh-1")
        manager.updateTokens("access-2", "refresh-1")
        assertEquals(SessionState.AUTHENTICATED, manager.state.value)
        assertEquals("access-2", manager.accessToken())
    }
}

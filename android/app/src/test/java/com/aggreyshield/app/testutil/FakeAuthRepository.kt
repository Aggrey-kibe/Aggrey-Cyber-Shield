package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.domain.model.MfaSetup
import com.aggreyshield.app.domain.repository.AuthRepository

/**
 * Controllable fake used by ViewModel tests. Each call records its arguments so tests can
 * assert on them, and returns whatever [nextResult] is currently set to.
 */
class FakeAuthRepository : AuthRepository {
    var nextResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var lastLoginMfaCode: String? = null
    var logoutCalled: Boolean = false

    override suspend fun login(email: String, password: String, mfaCode: String?): ApiResult<Unit> {
        lastLoginEmail = email
        lastLoginPassword = password
        lastLoginMfaCode = mfaCode
        return nextResult
    }

    override suspend fun register(email: String, password: String, fullName: String): ApiResult<Unit> = nextResult
    override suspend fun verifyEmail(email: String, code: String): ApiResult<Unit> = nextResult
    override suspend fun resendVerification(email: String): ApiResult<Unit> = nextResult
    override suspend fun forgotPassword(email: String): ApiResult<Unit> = nextResult
    override suspend fun resetPassword(code: String, newPassword: String): ApiResult<Unit> = nextResult
    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> = nextResult

    override suspend fun startMfaSetup(): ApiResult<MfaSetup> =
        ApiResult.Failure(AppError.NotFound())

    override suspend fun confirmMfa(code: String): ApiResult<Unit> = nextResult

    override suspend fun logout() {
        logoutCalled = true
    }
}

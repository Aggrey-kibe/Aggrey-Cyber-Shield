package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.MfaSetup
import com.aggreyshield.app.domain.repository.AuthRepository

class FakeAuthRepository : AuthRepository {
    var nextResult: ApiResult<Unit> = ApiResult.Success(Unit)
    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var loginCallCount: Int = 0

    override suspend fun login(email: String, password: String, mfaCode: String?): ApiResult<Unit> {
        lastLoginEmail = email
        lastLoginPassword = password
        loginCallCount++
        return nextResult
    }

    override suspend fun register(email: String, password: String, fullName: String): ApiResult<Unit> = nextResult
    override suspend fun verifyEmail(email: String, code: String): ApiResult<Unit> = nextResult
    override suspend fun resendVerification(email: String): ApiResult<Unit> = nextResult
    override suspend fun forgotPassword(email: String): ApiResult<Unit> = nextResult
    override suspend fun resetPassword(code: String, newPassword: String): ApiResult<Unit> = nextResult
    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> = nextResult
    override suspend fun startMfaSetup(): ApiResult<MfaSetup> = error("startMfaSetup is not used by LoginScreenTest")
    override suspend fun confirmMfa(code: String): ApiResult<Unit> = nextResult
    override suspend fun logout() = Unit
}

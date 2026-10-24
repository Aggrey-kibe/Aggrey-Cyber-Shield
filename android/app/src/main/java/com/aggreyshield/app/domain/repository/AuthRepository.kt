package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.MfaSetup

interface AuthRepository {
    suspend fun login(email: String, password: String, mfaCode: String?): ApiResult<Unit>
    suspend fun register(email: String, password: String, fullName: String): ApiResult<Unit>
    suspend fun verifyEmail(email: String, code: String): ApiResult<Unit>
    suspend fun resendVerification(email: String): ApiResult<Unit>
    suspend fun forgotPassword(email: String): ApiResult<Unit>
    suspend fun resetPassword(code: String, newPassword: String): ApiResult<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>
    suspend fun startMfaSetup(): ApiResult<MfaSetup>
    suspend fun confirmMfa(code: String): ApiResult<Unit>

    /** Tells the server to revoke the refresh token (best effort), then always wipes local tokens. */
    suspend fun logout()
}

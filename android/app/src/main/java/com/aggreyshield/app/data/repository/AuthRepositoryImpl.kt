package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.data.api.AuthApi
import com.aggreyshield.app.data.model.ChangePasswordRequestDto
import com.aggreyshield.app.data.model.CodeRequestDto
import com.aggreyshield.app.data.model.EmailRequestDto
import com.aggreyshield.app.data.model.LoginRequestDto
import com.aggreyshield.app.data.model.RegisterRequestDto
import com.aggreyshield.app.data.model.ResetPasswordRequestDto
import com.aggreyshield.app.data.model.VerifyEmailRequestDto
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.domain.model.MfaSetup
import com.aggreyshield.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import com.aggreyshield.app.data.model.LogoutRequestDto
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val apiCaller: ApiCaller,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun login(email: String, password: String, mfaCode: String?): ApiResult<Unit> {
        val result = apiCaller.call {
            authApi.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password,
                    mfaCode = mfaCode?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
        return when (result) {
            is ApiResult.Success -> {
                val refreshToken = result.data.refreshToken
                if (refreshToken.isNullOrBlank()) {
                    ApiResult.Failure(AppError.InvalidResponse)
                } else {
                    sessionManager.startSession(result.data.accessToken, refreshToken)
                    ApiResult.Success(Unit)
                }
            }
            is ApiResult.Failure -> result
        }
    }

    override suspend fun register(email: String, password: String, fullName: String): ApiResult<Unit> =
        apiCaller.call { authApi.register(RegisterRequestDto(email.trim(), password, fullName.trim())) }

    override suspend fun verifyEmail(email: String, code: String): ApiResult<Unit> =
        apiCaller.call { authApi.verifyEmail(VerifyEmailRequestDto(email.trim(), code.trim())) }

    override suspend fun resendVerification(email: String): ApiResult<Unit> =
        apiCaller.call { authApi.resendVerification(EmailRequestDto(email.trim())) }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        apiCaller.call { authApi.forgotPassword(EmailRequestDto(email.trim())) }

    override suspend fun resetPassword(code: String, newPassword: String): ApiResult<Unit> =
        apiCaller.call { authApi.resetPassword(ResetPasswordRequestDto(code.trim(), newPassword)) }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        apiCaller.call { authApi.changePassword(ChangePasswordRequestDto(currentPassword, newPassword)) }

    override suspend fun startMfaSetup(): ApiResult<MfaSetup> =
        apiCaller.call { authApi.startMfaSetup().toDomain() }

    override suspend fun confirmMfa(code: String): ApiResult<Unit> =
        apiCaller.call { authApi.confirmMfa(CodeRequestDto(code.trim())) }

    override suspend fun logout() {
        try {
            sessionManager.refreshToken()?.let { refreshToken ->
                authApi.logout(LogoutRequestDto(refreshToken))
            }        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best effort: the server-side refresh token is revoked when reachable, but the
            // local session always ends below regardless, so the user is never stuck signed in.
        } finally {
            sessionManager.endSession(expired = false)
        }
    }
}

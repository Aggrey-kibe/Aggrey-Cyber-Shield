package com.aggreyshield.app.data.api



import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.ChangePasswordRequestDto
import com.aggreyshield.app.data.model.CodeRequestDto
import com.aggreyshield.app.data.model.EmailRequestDto
import com.aggreyshield.app.data.model.LoginRequestDto
import com.aggreyshield.app.data.model.MfaSetupDto
import com.aggreyshield.app.data.model.RegisterRequestDto
import com.aggreyshield.app.data.model.ResetPasswordRequestDto
import com.aggreyshield.app.data.model.TokenResponseDto
import com.aggreyshield.app.data.model.VerifyEmailRequestDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.aggreyshield.app.data.model.LogoutRequestDto

/**
 * Calls made without an existing session. Every call carries the no-auth marker header so
 * AuthInterceptor does not attach a (possibly missing or expired) access token to them.
 */
interface AuthApi {

    @POST(ApiEndpoints.AUTH_REGISTER)
    suspend fun register(
        @Body request: RegisterRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    )

    @POST(ApiEndpoints.AUTH_LOGIN)
    suspend fun login(
        @Body request: LoginRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    ): TokenResponseDto

    @POST(ApiEndpoints.AUTH_LOGOUT)
    suspend fun logout(
        @Body request: LogoutRequestDto,
    )

    @POST(ApiEndpoints.AUTH_VERIFY_EMAIL)
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    )

    @POST(ApiEndpoints.AUTH_RESEND_VERIFICATION)
    suspend fun resendVerification(
        @Body request: EmailRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    )

    @POST(ApiEndpoints.AUTH_FORGOT_PASSWORD)
    suspend fun forgotPassword(
        @Body request: EmailRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    )

    @POST(ApiEndpoints.AUTH_RESET_PASSWORD)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    )

    @POST(ApiEndpoints.AUTH_CHANGE_PASSWORD)
    suspend fun changePassword(@Body request: ChangePasswordRequestDto)

    @POST(ApiEndpoints.AUTH_MFA_SETUP)
    suspend fun startMfaSetup(): MfaSetupDto

    @POST(ApiEndpoints.AUTH_MFA_VERIFY)
    suspend fun confirmMfa(@Body request: CodeRequestDto)
}

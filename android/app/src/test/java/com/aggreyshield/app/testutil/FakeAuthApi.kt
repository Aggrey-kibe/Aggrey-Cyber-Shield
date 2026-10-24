
package com.aggreyshield.app.testutil

import com.aggreyshield.app.data.api.AuthApi
import com.aggreyshield.app.data.model.ChangePasswordRequestDto
import com.aggreyshield.app.data.model.CodeRequestDto
import com.aggreyshield.app.data.model.EmailRequestDto
import com.aggreyshield.app.data.model.LoginRequestDto
import com.aggreyshield.app.data.model.LogoutRequestDto
import com.aggreyshield.app.data.model.MfaSetupDto
import com.aggreyshield.app.data.model.RegisterRequestDto
import com.aggreyshield.app.data.model.ResetPasswordRequestDto
import com.aggreyshield.app.data.model.TokenResponseDto
import com.aggreyshield.app.data.model.VerifyEmailRequestDto

class FakeAuthApi : AuthApi {
    var nextLoginResponse: TokenResponseDto =
        TokenResponseDto(
            accessToken = "access",
            refreshToken = "refresh",
        )

    var lastLoginRequest: LoginRequestDto? = null

    override suspend fun register(
        request: RegisterRequestDto,
        noAuth: Boolean,
    ) = Unit

    override suspend fun login(
        request: LoginRequestDto,
        noAuth: Boolean,
    ): TokenResponseDto {
        lastLoginRequest = request
        return nextLoginResponse
    }

    override suspend fun logout(
        request: LogoutRequestDto,
    ) = Unit

    override suspend fun verifyEmail(
        request: VerifyEmailRequestDto,
        noAuth: Boolean,
    ) = Unit

    override suspend fun resendVerification(
        request: EmailRequestDto,
        noAuth: Boolean,
    ) = Unit

    override suspend fun forgotPassword(
        request: EmailRequestDto,
        noAuth: Boolean,
    ) = Unit

    override suspend fun resetPassword(
        request: ResetPasswordRequestDto,
        noAuth: Boolean,
    ) = Unit

    override suspend fun changePassword(
        request: ChangePasswordRequestDto,
    ) = Unit

    override suspend fun startMfaSetup(): MfaSetupDto =
        MfaSetupDto(
            secret = "SECRET",
            otpauthUri = null,
        )

    override suspend fun confirmMfa(
        request: CodeRequestDto,
    ) = Unit
}

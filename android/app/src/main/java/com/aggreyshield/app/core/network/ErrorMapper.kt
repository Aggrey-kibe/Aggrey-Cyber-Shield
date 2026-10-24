package com.aggreyshield.app.core.network

import com.aggreyshield.app.core.common.AppError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Error format returned by the AGGREY SHIELD backend: {"error": {"code", "message", "details": [...]}}. */
@Serializable
data class ErrorEnvelopeDto(val error: ErrorBodyDto? = null)

@Serializable
data class ErrorBodyDto(
    val code: String? = null,
    val message: String? = null,
    val details: List<ErrorDetailDto> = emptyList(),
)

@Serializable
data class ErrorDetailDto(val field: String? = null, val message: String? = null)

/** Turns exceptions and HTTP failures into typed [AppError] values with safe user messages. */
object ErrorMapper {

    private const val MAX_SERVER_MESSAGE_LENGTH = 200

    fun fromThrowable(throwable: Throwable, isOnline: Boolean): AppError = when (throwable) {
        is HttpException -> fromHttp(
            code = throwable.code(),
            body = readBody(throwable),
            retryAfter = throwable.response()?.headers()?.get("Retry-After"),
        )
        is SocketTimeoutException -> AppError.Timeout
        is SSLException -> AppError.SecureConnectionFailed
        is UnknownHostException -> if (isOnline) AppError.ServerUnavailable else AppError.NoInternet
        is ConnectException -> if (isOnline) AppError.ServerUnavailable else AppError.NoInternet
        is IOException -> if (isOnline) AppError.ServerUnavailable else AppError.NoInternet
        is SerializationException -> AppError.InvalidResponse
        else -> AppError.Unknown
    }

    fun fromHttp(code: Int, body: String?, retryAfter: String? = null): AppError {
        val envelope = parseEnvelope(body)?.error
        val serverMessage = envelope?.message
            ?.takeIf { it.isNotBlank() && it.length <= MAX_SERVER_MESSAGE_LENGTH }
        val apiCode = envelope?.code

        return when (code) {
            401 -> AppError.Unauthorized(serverMessage, apiCode)
            403 -> AppError.Forbidden(serverMessage, apiCode)
            404 -> AppError.NotFound(apiCode)
            422 -> AppError.Validation(fieldErrors(envelope))
            429 -> AppError.RateLimited(retryAfter?.trim()?.toIntOrNull())
            502, 503, 504 -> AppError.ServerUnavailable
            in 500..599 -> AppError.Server(code)
            else -> AppError.Client(code, serverMessage, apiCode)
        }
    }

    /** Server field names look like "body.email"; the form only needs the last part ("email"). */
    private fun fieldErrors(envelope: ErrorBodyDto?): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        envelope?.details?.forEach { detail ->
            val field = detail.field?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
            val message = detail.message?.takeIf { it.isNotBlank() && it.length <= MAX_SERVER_MESSAGE_LENGTH }
            if (field != null && message != null && field !in result) result[field] = message
        }
        return result
    }

    private fun parseEnvelope(body: String?): ErrorEnvelopeDto? {
        if (body.isNullOrBlank()) return null
        return try {
            AppJson.instance.decodeFromString(ErrorEnvelopeDto.serializer(), body)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun readBody(exception: HttpException): String? = try {
        exception.response()?.errorBody()?.string()
    } catch (e: IOException) {
        null
    }
}

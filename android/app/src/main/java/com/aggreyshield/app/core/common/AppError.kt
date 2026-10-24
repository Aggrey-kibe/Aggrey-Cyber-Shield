package com.aggreyshield.app.core.common

/**
 * Every failure the UI can show. [userMessage] is written for end users and never contains
 * stack traces, server internals or raw backend text for server-side (5xx) errors.
 */
sealed class AppError(val userMessage: String, val apiCode: String? = null) {

    data object NoInternet : AppError("No internet connection. Check your Wi-Fi or mobile data and try again.")

    data object Timeout : AppError("The server took too long to respond. Please try again.")

    data object ServerUnavailable : AppError("Can't reach the AGGREY SHIELD server right now. Please try again later.")

    data object SecureConnectionFailed : AppError("A secure connection to the server could not be established.")

    data object InvalidResponse : AppError("The server sent a response this app could not understand.")

    data class Unauthorized(val serverMessage: String? = null, val code: String? = null) :
        AppError(serverMessage ?: "Your session has expired or your credentials are incorrect. Please sign in again.", code)

    data class Forbidden(val serverMessage: String? = null, val code: String? = null) :
        AppError(serverMessage ?: "You don't have permission to do that.", code)

    data class NotFound(val code: String? = null) :
        AppError("This feature isn't available on the server yet, or the item no longer exists.", code)

    /** HTTP 422. [fieldErrors] maps a request field name (for example "email") to a message. */
    data class Validation(val fieldErrors: Map<String, String>) :
        AppError("Please check the highlighted fields and try again.", "VALIDATION_ERROR")

    data class RateLimited(val retryAfterSeconds: Int? = null) :
        AppError(
            if (retryAfterSeconds != null) "Too many requests. Please wait $retryAfterSeconds seconds and try again."
            else "Too many requests. Please wait a moment and try again.",
            "RATE_LIMITED",
        )

    /** Other 4xx responses such as 400 and 409. */
    data class Client(val httpStatus: Int, val serverMessage: String? = null, val code: String? = null) :
        AppError(serverMessage ?: "The request could not be completed. Please check your input and try again.", code)

    data class Server(val httpStatus: Int) :
        AppError("Something went wrong on our side. Please try again later.", "SERVER_ERROR")

    data object Unknown : AppError("Something went wrong. Please try again.")

    /** Message for a single form field, or null when the server did not flag that field. */
    fun fieldError(field: String): String? = (this as? Validation)?.fieldErrors?.get(field)
}

package com.aggreyshield.app.core.common

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Result of checking a URL typed by the user. */
sealed interface UrlCheck {
    data class Valid(val url: String) : UrlCheck
    data class Invalid(val reason: String) : UrlCheck
}

/**
 * Client-side input validation. Every function returns an error message, or null when the
 * input is acceptable. The server always validates again; this only gives faster feedback.
 */
object InputValidator {
    const val PASSWORD_MIN_LENGTH = 10
    const val NAME_MAX_LENGTH = 100
    const val TITLE_MIN_LENGTH = 5
    const val TITLE_MAX_LENGTH = 120
    const val DESCRIPTION_MIN_LENGTH = 10
    const val DESCRIPTION_MAX_LENGTH = 4000
    const val MESSAGE_MAX_LENGTH = 5000
    const val URL_MAX_LENGTH = 2048
    const val COMMENT_MAX_LENGTH = 2000

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(value: String): String? {
        val email = value.trim()
        return when {
            email.isEmpty() -> "Enter your email address."
            email.length > 254 -> "That email address is too long."
            !EMAIL_REGEX.matches(email) -> "Enter a valid email address."
            else -> null
        }
    }

    fun validateFullName(value: String): String? {
        val name = value.trim()
        return when {
            name.length < 2 -> "Enter your full name."
            name.length > NAME_MAX_LENGTH -> "Name must be at most $NAME_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateLoginPassword(value: String): String? =
        if (value.isEmpty()) "Enter your password." else null

    fun validateNewPassword(value: String): String? = when {
        value.length < PASSWORD_MIN_LENGTH -> "Use at least $PASSWORD_MIN_LENGTH characters."
        value.none { it.isUpperCase() } -> "Include at least one uppercase letter."
        value.none { it.isLowerCase() } -> "Include at least one lowercase letter."
        value.none { it.isDigit() } -> "Include at least one number."
        else -> null
    }

    fun validatePasswordMatch(password: String, confirmation: String): String? =
        if (password != confirmation) "Passwords do not match." else null

    fun validateCode(value: String): String? {
        val code = value.trim()
        return when {
            code.isEmpty() -> "Enter the code from your email."
            code.length < 4 || code.length > 128 -> "That code doesn't look right."
            code.any { it.isWhitespace() } -> "The code cannot contain spaces."
            else -> null
        }
    }

    fun validateMfaCode(value: String): String? =
        if (value.trim().length in 6..8 && value.trim().all { it.isDigit() }) null else "Enter the 6-digit code from your authenticator app."

    fun validateIncidentTitle(value: String): String? {
        val title = value.trim()
        return when {
            title.length < TITLE_MIN_LENGTH -> "Title must be at least $TITLE_MIN_LENGTH characters."
            title.length > TITLE_MAX_LENGTH -> "Title must be at most $TITLE_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateIncidentDescription(value: String): String? {
        val text = value.trim()
        return when {
            text.length < DESCRIPTION_MIN_LENGTH -> "Describe what happened in at least $DESCRIPTION_MIN_LENGTH characters."
            text.length > DESCRIPTION_MAX_LENGTH -> "Description must be at most $DESCRIPTION_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateMessageContent(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Paste the message you want to analyze."
            text.length > MESSAGE_MAX_LENGTH -> "Messages can be at most $MESSAGE_MAX_LENGTH characters."
            else -> null
        }
    }

    fun validateComment(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Write a comment first."
            text.length > COMMENT_MAX_LENGTH -> "Comments can be at most $COMMENT_MAX_LENGTH characters."
            else -> null
        }
    }

    /**
     * Checks a URL or domain typed by the user. Input without a scheme is submitted with
     * "http://" in front so the backend receives a complete URL; the text is otherwise unchanged.
     * Only http and https addresses are accepted.
     */
    fun checkUrl(input: String): UrlCheck {
        val text = input.trim()
        if (text.isEmpty()) return UrlCheck.Invalid("Enter a URL or domain to scan.")
        if (text.length > URL_MAX_LENGTH) return UrlCheck.Invalid("That address is too long.")
        if (text.any { it.isWhitespace() }) return UrlCheck.Invalid("A web address cannot contain spaces.")

        val hasScheme = text.contains("://")
        if (hasScheme) {
            val scheme = text.substringBefore("://").lowercase()
            if (scheme != "http" && scheme != "https") {
                return UrlCheck.Invalid("Only http and https addresses can be scanned.")
            }
        }
        val candidate = if (hasScheme) text else "http://$text"
        val parsed = candidate.toHttpUrlOrNull() ?: return UrlCheck.Invalid("That doesn't look like a valid web address.")
        if (!parsed.host.contains('.') && !parsed.host.contains(':')) {
            return UrlCheck.Invalid("Enter a full domain such as example.com.")
        }
        return UrlCheck.Valid(candidate)
    }

    /** Local strength hint for the password field. This is only a UI hint; the server decides what is accepted. */
    fun passwordStrength(value: String): PasswordStrength {
        if (value.isEmpty()) return PasswordStrength.NONE
        var score = 0
        if (value.length >= PASSWORD_MIN_LENGTH) score++
        if (value.length >= 14) score++
        if (value.any { it.isUpperCase() } && value.any { it.isLowerCase() }) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.FAIR
            else -> PasswordStrength.STRONG
        }
    }
}

enum class PasswordStrength(val label: String, val fraction: Float) {
    NONE("", 0f),
    WEAK("Weak", 0.33f),
    FAIR("Fair", 0.66f),
    STRONG("Strong", 1f),
}

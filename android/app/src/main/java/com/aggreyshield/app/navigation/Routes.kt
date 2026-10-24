package com.aggreyshield.app.navigation

import android.net.Uri

/**
 * Every navigation route in the app, as plain strings. Screens that need an argument expose a
 * `NavType` argument list on their `composable()` registration in [AppNavHost] and a builder
 * function here so call sites never hand-assemble a route string.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY_EMAIL_PATTERN = "verify-email/{email}"
    const val FORGOT_PASSWORD = "forgot-password"
    const val RESET_PASSWORD_PATTERN = "REDACTED"


    const val DASHBOARD = "dashboard"
    const val SECURITY_SCORE = "security-score"

    const val SCANNER_HUB = "scanner"
    const val URL_SCANNER = "scanner/url"
    const val MESSAGE_ANALYZER = "scanner/message"

    const val INCIDENT_LIST = "incidents"
    const val INCIDENT_REPORT = "incidents/report"
    const val INCIDENT_DETAIL_PATTERN = "incidents/{incidentId}"

    const val ALERTS = "alerts"
    const val THREAT_INTEL = "threat-intel"

    const val TRAINING_LIST = "training"
    const val COURSE_DETAIL_PATTERN = "training/{courseId}"
    const val TRAINING_RESULTS = "training-results"

    const val NOTIFICATIONS = "notifications"

    const val PROFILE = "profile"
    const val ACCOUNT_SECURITY = "account-security"
    const val CHANGE_PASSWORD = "change-password"
    const val MFA_SETUP = "mfa-setup"

    const val ORGANIZATION = "organization"
    const val ORGANIZATION_MEMBERS = "organization/members"

    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val LOGOUT_CONFIRM = "logout-confirm"

    const val ARG_EMAIL = "email"
    const val ARG_RESET_TOKEN = "token"
    const val ARG_INCIDENT_ID = "incidentId"
    const val ARG_COURSE_ID = "courseId"

    fun verifyEmail(email: String) = "verify-email/${Uri.encode(email)}"
    fun resetPassword(token: String) = "reset-password/${Uri.encode(token)}"
    fun incidentDetail(id: String) = "incidents/${Uri.encode(id)}"
    fun courseDetail(id: String) = "training/${Uri.encode(id)}"

    /** Routes reachable before or without a session. Used to decide the start destination. */
    val PRE_AUTH_ROUTES = setOf(
        SPLASH,
        ONBOARDING,
        LOGIN,
        REGISTER,
        VERIFY_EMAIL_PATTERN,
        FORGOT_PASSWORD,
        RESET_PASSWORD_PATTERN,
    )
}

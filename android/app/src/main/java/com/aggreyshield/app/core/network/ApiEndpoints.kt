package com.aggreyshield.app.core.network

/**
 * Every backend path used by the app, in one place. Retrofit interfaces reference these
 * constants, so changing a path here changes it everywhere.
 *
 * Paths have no leading slash (Retrofit resolves them against the base URL) and no
 * trailing slash. STATUS of each group on the backend:
 *   - HEALTH          : exists today (backend Phase 1).
 *   - everything else : PLANNED. The Android side is complete, but the endpoint must be
 *                       implemented on the FastAPI backend before the feature works.
 *                       Until then the app shows "not available on the server yet".
 */
object ApiEndpoints {
    const val API_V1 = "api/v1/"

    /** Requests carrying this header are sent without an Authorization header (login, register, ...). */
    const val HEADER_NO_AUTH = "X-No-Auth"
    const val NO_AUTH = "$HEADER_NO_AUTH: true"

    // --- Exists on the backend today ---
    const val HEALTH = API_V1 + "health"

    // --- Authentication (planned) ---
    const val AUTH_REGISTER = API_V1 + "auth/register"
    const val AUTH_LOGIN = API_V1 + "auth/login"
    const val AUTH_REFRESH = API_V1 + "auth/refresh"
    const val AUTH_LOGOUT = API_V1 + "auth/logout"
    const val AUTH_VERIFY_EMAIL = API_V1 + "auth/verify-email"
    const val AUTH_RESEND_VERIFICATION = API_V1 + "auth/resend-verification"
    const val AUTH_FORGOT_PASSWORD = API_V1 + "auth/forgot-password"
    const val AUTH_RESET_PASSWORD = API_V1 + "auth/reset-password"
    const val AUTH_CHANGE_PASSWORD = API_V1 + "auth/change-password"
    const val AUTH_MFA_SETUP = API_V1 + "auth/mfa/setup"
    const val AUTH_MFA_VERIFY = API_V1 + "auth/mfa/verify"

    // --- Users and organizations (planned) ---
    const val USERS_ME = API_V1 + "users/me"
    const val USERS_ME_SECURITY_SCORE = API_V1 + "users/me/security-score"
    const val ORGANIZATION_ME = API_V1 + "organizations/me"
    const val ORGANIZATION_MEMBERS = API_V1 + "organizations/me/members"

    // --- Scanning (planned) ---
    const val SCAN_URL = API_V1 + "scan/url"
    const val SCAN_MESSAGE = API_V1 + "scan/message"
    const val SCAN_HISTORY = API_V1 + "scan/history"

    // --- Incidents (planned) ---
    const val INCIDENTS = API_V1 + "incidents"
    const val INCIDENT_BY_ID = INCIDENTS + "/{id}"
    const val INCIDENT_COMMENTS = INCIDENT_BY_ID + "/comments"
    const val INCIDENT_EVIDENCE = INCIDENT_BY_ID + "/evidence"

    // --- Alerts (planned) ---
    const val ALERTS = API_V1 + "alerts"
    const val ALERT_ACKNOWLEDGE = ALERTS + "/{id}/acknowledge"

    // --- Threat intelligence (planned) ---
    const val THREAT_INTEL_INDICATORS = API_V1 + "threat-intel/indicators"

    // --- Training (planned) ---
    const val TRAINING_COURSES = API_V1 + "training/courses"
    const val TRAINING_COURSE_BY_ID = TRAINING_COURSES + "/{id}"
    const val TRAINING_LESSON_COMPLETE = TRAINING_COURSE_BY_ID + "/lessons/{lessonId}/complete"
    const val TRAINING_QUIZ_SUBMIT = TRAINING_COURSE_BY_ID + "/quiz"
    const val TRAINING_RESULTS = API_V1 + "training/results"

    // --- Notifications (planned) ---
    const val NOTIFICATIONS = API_V1 + "notifications"
    const val NOTIFICATION_READ = NOTIFICATIONS + "/{id}/read"
    const val NOTIFICATIONS_READ_ALL = NOTIFICATIONS + "/read-all"
}

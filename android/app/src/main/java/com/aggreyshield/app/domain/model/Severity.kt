package com.aggreyshield.app.domain.model

/** Risk level of a scan, and severity of an alert, incident or indicator. [UNKNOWN] means the server sent no usable value. */
enum class Severity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical"),
    UNKNOWN("Unknown");

    companion object {
        fun fromApi(value: String?): Severity =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

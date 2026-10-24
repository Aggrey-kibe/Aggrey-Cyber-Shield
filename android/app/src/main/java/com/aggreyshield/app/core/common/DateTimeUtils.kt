package com.aggreyshield.app.core.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeUtils {

    /**
     * Parses timestamps as produced by a FastAPI/Pydantic backend. Accepts values with an
     * offset ("2026-01-01T10:00:00Z", "...+00:00") and naive values, which are treated as UTC.
     * Returns null when the value is missing or not a valid timestamp; it never invents a time.
     */
    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        val text = value.trim()
        return try {
            OffsetDateTime.parse(text).toInstant()
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(text).toInstant(ZoneOffset.UTC)
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }

    fun format(instant: Instant?, zone: ZoneId = ZoneId.systemDefault()): String {
        if (instant == null) return "Unknown time"
        return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(zone).format(instant)
    }
}

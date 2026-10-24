package com.aggreyshield.app.core.network

import kotlinx.serialization.json.Json

/** The single JSON configuration used for every request and response. */
object AppJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}

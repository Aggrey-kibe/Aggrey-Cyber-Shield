package com.aggreyshield.app.domain.model

/** One page of a list returned by the backend. [total] is null when the server did not report it. */
data class Page<T>(val items: List<T>, val total: Int?)

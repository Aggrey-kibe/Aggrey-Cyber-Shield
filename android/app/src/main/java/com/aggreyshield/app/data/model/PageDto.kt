package com.aggreyshield.app.data.model

import com.aggreyshield.app.domain.model.Page
import kotlinx.serialization.Serializable

/** List envelope used by every list endpoint: {"items": [...], "total": 12}. */
@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    val total: Int? = null,
)

fun <D, T> PageDto<D>.toPage(transform: (D) -> T): Page<T> = Page(items.map(transform), total)

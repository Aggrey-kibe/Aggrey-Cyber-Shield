package com.aggreyshield.app.core.common

/** State of one piece of remote data on screen. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Loaded<T>(val data: T) : Loadable<T>
    data class Failed(val error: AppError) : Loadable<Nothing>
}

fun <T> ApiResult<T>.toLoadable(): Loadable<T> = when (this) {
    is ApiResult.Success -> Loadable.Loaded(data)
    is ApiResult.Failure -> Loadable.Failed(error)
}

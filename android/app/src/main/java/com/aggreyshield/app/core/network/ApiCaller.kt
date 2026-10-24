package com.aggreyshield.app.core.network

import com.aggreyshield.app.core.common.ApiResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a Retrofit call and converts every failure into an [ApiResult.Failure] with a typed
 * [com.aggreyshield.app.core.common.AppError]. Repositories use this for every request, so
 * error handling is identical across the whole app.
 */
@Singleton
class ApiCaller @Inject constructor(private val networkMonitor: NetworkMonitor) {

    suspend fun <T> call(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Failure(ErrorMapper.fromThrowable(e, networkMonitor.isCurrentlyOnline()))
    }
}

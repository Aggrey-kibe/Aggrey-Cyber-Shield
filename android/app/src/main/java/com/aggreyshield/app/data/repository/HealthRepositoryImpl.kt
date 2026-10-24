package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.HealthApi
import com.aggreyshield.app.domain.model.ServerHealth
import com.aggreyshield.app.domain.repository.HealthRepository
import okhttp3.HttpUrl
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthApi: HealthApi,
    private val apiCaller: ApiCaller,
    @Named("apiRetrofit") private val retrofit: Retrofit,
) : HealthRepository {

    override suspend fun check(): ApiResult<ServerHealth> = apiCaller.call {
        val body = healthApi.check()
        ServerHealth(
            serverUrl = (retrofit.baseUrl() as HttpUrl).toString(),
            apiReachable = true,
            databaseConnected = body.database == "connected",
            environment = body.environment,
            appVersion = body.appVersion,
        )
    }
}

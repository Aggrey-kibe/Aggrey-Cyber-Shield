package com.aggreyshield.app.di

import com.aggreyshield.app.BuildConfig
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.core.network.AppJson
import com.aggreyshield.app.core.network.AuthInterceptor
import com.aggreyshield.app.core.network.BaseUrlInterceptor
import com.aggreyshield.app.core.network.TokenAuthenticator
import com.aggreyshield.app.data.api.AlertApi
import com.aggreyshield.app.data.api.AuthApi
import com.aggreyshield.app.data.api.HealthApi
import com.aggreyshield.app.data.api.IncidentApi
import com.aggreyshield.app.data.api.NotificationApi
import com.aggreyshield.app.data.api.ScanApi
import com.aggreyshield.app.data.api.ThreatIntelApi
import com.aggreyshield.app.data.api.TokenRefreshApi
import com.aggreyshield.app.data.api.TrainingApi
import com.aggreyshield.app.data.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Two independent Retrofit/OkHttp stacks:
 *  - [QUALIFIER_AUTH_RETROFIT]: no access token attached, no 401-refresh logic. Used for
 *    login/register/refresh/forgot-password, where a 401 means "wrong credentials", not
 *    "session expired".
 *  - [QUALIFIER_API_RETROFIT]: attaches the access token and retries once through
 *    [TokenAuthenticator] on a 401. Used for every endpoint that needs a signed-in user.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = AppJson.instance

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // BASIC logs only method/URL/status/duration - never headers or bodies, so access
        // tokens, passwords, and submitted scan/message content never reach Logcat.
        level = if (BuildConfig.BUILD_ENVIRONMENT == "production") {
            HttpLoggingInterceptor.Level.NONE
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    @Named(QUALIFIER_AUTH_CLIENT)
    fun provideAuthOkHttpClient(
        logging: HttpLoggingInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .apply { if (BuildConfig.ALLOW_SERVER_OVERRIDE) addInterceptor(baseUrlInterceptor) }
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @Named(QUALIFIER_API_CLIENT)
    fun provideApiOkHttpClient(
        logging: HttpLoggingInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply { if (BuildConfig.ALLOW_SERVER_OVERRIDE) addInterceptor(baseUrlInterceptor) }
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @Named(QUALIFIER_AUTH_RETROFIT)
    fun provideAuthRetrofit(
        @Named(QUALIFIER_AUTH_CLIENT) client: OkHttpClient,
        json: Json
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    @Provides
    @Singleton
    @Named(QUALIFIER_API_RETROFIT)
    fun provideApiRetrofit(@Named(QUALIFIER_API_CLIENT) client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiCaller(networkMonitor: com.aggreyshield.app.core.network.NetworkMonitor): ApiCaller = ApiCaller(networkMonitor)

    @Provides
    @Singleton
    fun provideAuthApi(@Named(QUALIFIER_AUTH_RETROFIT) retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTokenRefreshApi(@Named(QUALIFIER_AUTH_RETROFIT) retrofit: Retrofit): TokenRefreshApi =
        retrofit.create(TokenRefreshApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideScanApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): ScanApi = retrofit.create(ScanApi::class.java)

    @Provides
    @Singleton
    fun provideIncidentApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): IncidentApi = retrofit.create(IncidentApi::class.java)

    @Provides
    @Singleton
    fun provideAlertApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): AlertApi = retrofit.create(AlertApi::class.java)

    @Provides
    @Singleton
    fun provideThreatIntelApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): ThreatIntelApi =
        retrofit.create(ThreatIntelApi::class.java)

    @Provides
    @Singleton
    fun provideTrainingApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): TrainingApi = retrofit.create(TrainingApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApi(@Named(QUALIFIER_API_RETROFIT) retrofit: Retrofit): HealthApi = retrofit.create(HealthApi::class.java)
}

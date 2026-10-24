package com.aggreyshield.app.di

import javax.inject.Qualifier

/** A CoroutineScope that lives as long as the application process. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Qualifies the Retrofit/OkHttp stack used for login, register, refresh, forgot/reset-password. */
const val QUALIFIER_AUTH_CLIENT = "authOkHttp"
const val QUALIFIER_AUTH_RETROFIT = "authRetrofit"

/** Qualifies the Retrofit/OkHttp stack used for every endpoint that requires a signed-in session. */
const val QUALIFIER_API_CLIENT = "apiOkHttp"
const val QUALIFIER_API_RETROFIT = "apiRetrofit"

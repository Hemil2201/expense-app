package com.expensesplitter.app.data.remote

import com.expensesplitter.app.BuildConfig
import com.expensesplitter.app.data.local.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    fun createApiService(tokenStore: TokenStore): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            // Default 10s is too short for receipt/statement uploads, which
            // upload a file to Storage and then wait on an AI call
            // server-side — both sequential, both over the network.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = tokenStore.getToken()?.let { token ->
                    chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
                } ?: chain.request()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

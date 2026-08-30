package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val DEFAULT_BASE_URL = "http://pos.akbarmediagroup.me/"
    const val WEB_APP_URL = "http://toko.akbarmediagroup.me:4760"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var currentService: PosApiService? = null

    fun getService(baseUrl: String = DEFAULT_BASE_URL): PosApiService {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (currentService == null || currentBaseUrl != formattedUrl) {
            currentBaseUrl = formattedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            currentService = retrofit.create(PosApiService::class.java)
        }
        return currentService!!
    }
}

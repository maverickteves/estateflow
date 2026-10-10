package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class EstateApiClient private constructor(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("estateflow_backend_prefs", Context.MODE_PRIVATE)

  // Default address for the deployed Wasmer backend
  var baseUrl: String
    get() {
      val raw = prefs.getString("backend_url", DEFAULT_EMULATOR_URL) ?: DEFAULT_EMULATOR_URL
      return normalizeUrl(raw)
    }
    set(value) {
      val normalized = normalizeUrl(value)
      prefs.edit().putString("backend_url", normalized).apply()
      rebuildService(normalized)
    }

  val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .writeTimeout(8, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

  @Volatile
  private var currentService: EstateApiService = buildService(baseUrl)

  val service: EstateApiService
    get() = currentService

  private fun buildService(url: String): EstateApiService {
    val cleanUrl = if (url.endsWith("/")) url else "$url/"
    return Retrofit.Builder()
      .baseUrl(cleanUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(EstateApiService::class.java)
  }

  private fun rebuildService(newUrl: String) {
    currentService = buildService(newUrl)
  }

  companion object {
    const val DEFAULT_EMULATOR_URL = "https://estateflow-backend.wasmer.app/api/"
    const val DEFAULT_LAN_URL_TEMPLATE = "http://192.168.1.100/estateflow-backend/api/"

    @Volatile
    private var INSTANCE: EstateApiClient? = null

    fun normalizeUrl(raw: String): String {
      var trimmed = raw.trim()
      if (trimmed.isEmpty()) return DEFAULT_EMULATOR_URL
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        trimmed = "http://$trimmed"
      }
      while (trimmed.endsWith("/")) {
        trimmed = trimmed.removeSuffix("/")
      }
      // Wasmer serves this backend from the domain root. Ignore any local
      // deployment path that may have been appended to the saved URL.
      if (trimmed.contains(".wasmer.app")) {
        val authorityStart = trimmed.indexOf("://") + 3
        val pathStart = trimmed.indexOf('/', startIndex = authorityStart)
        val host = if (pathStart == -1) trimmed else trimmed.substring(0, pathStart)
        return "$host/api/"
      }
      return when {
        trimmed.endsWith("/estateflow-backend/api") -> "$trimmed/"
        trimmed.endsWith("/estateflow-backend") -> "$trimmed/api/"
        trimmed.endsWith("/api") -> "$trimmed/"
        else -> "$trimmed/estateflow-backend/api/"
      }
    }

    fun getInstance(context: Context): EstateApiClient {
      return INSTANCE ?: synchronized(this) {
        val instance = EstateApiClient(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }
}

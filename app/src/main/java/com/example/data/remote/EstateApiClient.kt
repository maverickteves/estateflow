package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class EstateApiClient private constructor(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("estateflow_backend_prefs", Context.MODE_PRIVATE)

  // The app always talks to the deployed backend. The address is not shown
  // or editable in the UI, so drop any address saved by older builds.
  val baseUrl: String = DEFAULT_EMULATOR_URL

  init {
    prefs.edit().remove("backend_url").apply()
  }

  /** Access token from the last sign-in, sent with every request so the backend knows who is calling. */
  var authToken: String?
    get() = prefs.getString(KEY_AUTH_TOKEN, null)
    set(value) {
      prefs.edit().apply {
        if (value.isNullOrBlank()) remove(KEY_AUTH_TOKEN) else putString(KEY_AUTH_TOKEN, value)
      }.apply()
    }

  val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .writeTimeout(8, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      val token = authToken
      val request = if (token != null) {
        chain.request().newBuilder().header("Authorization", "Bearer $token").build()
      } else {
        chain.request()
      }
      val response = chain.proceed(request)
      // An expired or revoked token is dropped so the user is asked to sign in again
      // instead of every request failing. Wrong-password replies from auth.php are not session errors.
      if (token != null && response.code == 401 && !request.url.encodedPath.endsWith("auth.php")) {
        authToken = null
      }
      response
    }
    .addInterceptor(HttpLoggingInterceptor().apply {
      // Request bodies carry passwords, so never log them outside debug builds.
      level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    })
    .build()

  val service: EstateApiService = buildService(baseUrl)

  private fun buildService(url: String): EstateApiService {
    val cleanUrl = if (url.endsWith("/")) url else "$url/"
    return Retrofit.Builder()
      .baseUrl(cleanUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(EstateApiService::class.java)
  }

  companion object {
    const val DEFAULT_EMULATOR_URL = "https://estateflow-backend.wasmer.app/api/"
    private const val KEY_AUTH_TOKEN = "auth_token"

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

package com.example.myapplication.network

import android.util.Log
import com.example.myapplication.BuildConfig
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    //  Para el EMULADOR, usar SIEMPRE 10.0.2.2 en lugar de 127.0.0.1
    private const val BASE_URL = "http://10.0.2.2:8000/api/"
    private const val MANUAL_CHECKOUT_TAG = "ManualCheckoutHttp"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val isManualCheckout =
                request.url.encodedPath.endsWith("/api/mobile/checkout")

            if (BuildConfig.DEBUG && isManualCheckout) {
                val requestBody = runCatching {
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    buffer.readUtf8()
                }.getOrElse { "<no se pudo leer el body: ${it.message}>" }
                val authorization = when {
                    request.header("Authorization") == null -> "<ausente>"
                    request.header("Authorization")!!.startsWith("Bearer ") -> "Bearer ***"
                    else -> "<formato inesperado>"
                }

                Log.d(
                    MANUAL_CHECKOUT_TAG,
                    "--> ${request.method} ${request.url}\n" +
                        "Authorization: $authorization\n" +
                        "Accept: ${request.header("Accept")}\n" +
                        "Content-Type: ${request.header("Content-Type") ?: request.body?.contentType()}\n" +
                        "Body: $requestBody"
                )
            }

            val response = chain.proceed(request)

            if (BuildConfig.DEBUG && isManualCheckout) {
                val responseBody = runCatching {
                    response.peekBody(1024L * 1024L).string()
                }.getOrElse { "<no se pudo leer la respuesta: ${it.message}>" }

                Log.d(
                    MANUAL_CHECKOUT_TAG,
                    "<-- HTTP ${response.code} ${response.request.url}\n" +
                        "Content-Type: ${response.header("Content-Type")}\n" +
                        "Body: $responseBody"
                )
            }

            response
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // debe terminar en /
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

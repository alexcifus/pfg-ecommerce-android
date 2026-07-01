package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    //  Para el EMULADOR, usar SIEMPRE 10.0.2.2 en lugar de 127.0.0.1
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // debe terminar en /
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

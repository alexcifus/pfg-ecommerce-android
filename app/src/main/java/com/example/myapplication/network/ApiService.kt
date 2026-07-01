package com.example.myapplication.network

import com.example.myapplication.model.CheckoutRequest
import com.example.myapplication.model.CheckoutResponse
import com.example.myapplication.model.HomeResponse
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login_ecommerce")
    suspend fun loginMobile(
        @Body body: LoginRequest
    ): LoginResponse

    @GET("mobile/products")
    suspend fun getMobileProducts(): HomeResponse

    @POST("mobile/checkout")
    suspend fun checkoutMobile(
        @Header("Authorization") token: String,
        @Body body: CheckoutRequest
    ): CheckoutResponse
}
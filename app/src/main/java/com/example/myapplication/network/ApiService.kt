package com.example.myapplication.network

import com.example.myapplication.model.CheckoutRequest
import com.example.myapplication.model.CheckoutResponse
import com.example.myapplication.model.HomeResponse
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.LoginResponse
import com.example.myapplication.model.MobilePayPalCaptureResponse
import com.example.myapplication.model.MobilePayPalOrderRequest
import com.example.myapplication.model.MobilePayPalOrderResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login_ecommerce")
    suspend fun loginMobile(
        @Body body: LoginRequest
    ): LoginResponse

    @GET("mobile/products")
    suspend fun getMobileProducts(): HomeResponse

    @POST("mobile/checkout")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun checkoutMobile(
        @Header("Authorization") token: String,
        @Body body: CheckoutRequest
    ): CheckoutResponse

    @POST("mobile/paypal/orders")
    suspend fun createMobilePayPalOrder(
        @Header("Authorization") token: String,
        @Body body: MobilePayPalOrderRequest
    ): MobilePayPalOrderResponse

    @POST("mobile/paypal/orders/{paypalOrderId}/capture")
    suspend fun captureMobilePayPalOrder(
        @Header("Authorization") token: String,
        @Path("paypalOrderId") paypalOrderId: String
    ): MobilePayPalCaptureResponse
}

package com.example.myapplication.model

data class MobilePayPalOrderRequest(
    val client_request_id: String,
    val items: List<MobilePayPalItemRequest>
)

data class MobilePayPalItemRequest(
    val product_id: Int,
    val quantity: Int
)

data class MobilePayPalOrderResponse(
    val message: Int,
    val message_text: String,
    val sale_id: Int,
    val paypal_order_id: String,
    val amount: String,
    val currency: String,
    val status: String
)

data class MobilePayPalCaptureResponse(
    val message: Int,
    val message_text: String,
    val sale_id: Int,
    val paypal_order_id: String?,
    val paypal_capture_id: String?,
    val n_transaccion: String?,
    val amount: String?,
    val currency: String?,
    val status: String,
    val idempotent: Boolean = false
)

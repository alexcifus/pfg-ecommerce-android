package com.example.myapplication.model

data class CheckoutRequest(
    val items: List<CheckoutItemRequest>,
    val total: Double,
    val method_payment: String? = null
)

data class CheckoutItemRequest(
    val product_id: Int,
    val quantity: Int,
    val price: Double
)

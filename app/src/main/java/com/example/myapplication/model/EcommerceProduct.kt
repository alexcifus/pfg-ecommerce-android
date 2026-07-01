package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class EcommerceProduct(
    val id: Int,
    val title: String,
    @SerializedName("price_eur") val priceEur: Int,
    val imagen: String,

    // Campos extra para la pantalla de detalle (opcionales)
    val resumen: String? = null,
    val brand: Brand? = null,
    @SerializedName("categorie_first") val categorieFirst: Category? = null
)

data class Brand(
    val id: Int,
    val name: String
)

data class Category(
    val id: Int,
    val name: String
)

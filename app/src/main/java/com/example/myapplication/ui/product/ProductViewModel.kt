package com.example.myapplication.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _products = MutableStateFlow<List<EcommerceProduct>>(emptyList())

    val loading = _loading.asStateFlow()
    val error = _error.asStateFlow()
    val products = _products.asStateFlow()

    fun loadProducts() {
        if (_products.value.isNotEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val response = ApiClient.api.getMobileProducts()
                _products.value = response.data
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar productos"
            } finally {
                _loading.value = false
            }
        }
    }
}

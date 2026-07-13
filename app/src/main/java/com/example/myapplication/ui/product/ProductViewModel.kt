package com.example.myapplication.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _products = MutableStateFlow<List<EcommerceProduct>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val loading = _loading.asStateFlow()
    val error = _error.asStateFlow()
    val products = _products.asStateFlow()
    val searchQuery = _searchQuery.asStateFlow()
    val filteredProducts = combine(
        _products,
        _searchQuery
    ) { products, query ->
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            products
        } else {
            products.filter { product ->
                product.title.contains(
                    normalizedQuery,
                    ignoreCase = true
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

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

package com.example.myapplication.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.CheckoutItemRequest
import com.example.myapplication.model.CheckoutRequest
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: EcommerceProduct,
    var quantity: Int
)

class CartViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _saleId = MutableStateFlow<Int?>(null)
    val saleId = _saleId.asStateFlow()

    fun addToCart(product: EcommerceProduct) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }

        if (index >= 0) {
            current[index].quantity++
        } else {
            current.add(CartItem(product, 1))
        }

        _items.value = current
    }

    fun removeItem(productId: Int) {
        _items.value = _items.value.filter { it.product.id != productId }
    }

    fun increaseQuantity(productId: Int) {
        val current = _items.value.toMutableList()
        val item = current.find { it.product.id == productId }
        item?.let {
            it.quantity++
            _items.value = current
        }
    }

    fun decreaseQuantity(productId: Int) {
        val current = _items.value.toMutableList()
        val item = current.find { it.product.id == productId }
        item?.let {
            it.quantity--
            if (it.quantity <= 0) {
                current.remove(it)
            }
            _items.value = current
        }
    }

    fun totalPrice(): Int {
        return _items.value.sumOf {
            (it.product.priceEur ?: 0) * it.quantity
        }
    }

    fun confirmOrder(token: String, methodPayment: String = "MOBILE_MANUAL") {
        if (_items.value.isEmpty()) {
            _message.value = "El carrito está vacío"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _message.value = null

            try {
                val request = CheckoutRequest(
                    items = _items.value.map {
                        CheckoutItemRequest(
                            product_id = it.product.id,
                            quantity = it.quantity,
                            price = (it.product.priceEur ?: 0).toDouble()
                        )
                    },
                    total = totalPrice().toDouble(),
                    method_payment = methodPayment
                )

                val response = ApiClient.api.checkoutMobile(
                    token = "Bearer $token",
                    body = request
                )

                if (response.message == 200) {
                    _items.value = emptyList()
                    _message.value = response.message_text
                    _saleId.value = response.sale_id
                } else {
                    _message.value = "No se pudo realizar el pedido"
                    _saleId.value = null
                }

            } catch (e: Exception) {
                _message.value = "Error al confirmar pedido: ${e.message}"
                _saleId.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

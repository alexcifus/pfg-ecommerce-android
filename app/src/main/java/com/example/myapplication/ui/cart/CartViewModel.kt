package com.example.myapplication.ui.cart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.CheckoutItemRequest
import com.example.myapplication.model.CheckoutRequest
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.model.MobilePayPalItemRequest
import com.example.myapplication.model.MobilePayPalOrderRequest
import com.example.myapplication.network.ApiClient
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class CartItem(
    val product: EcommerceProduct,
    val quantity: Int
)

sealed interface PayPalCheckoutState {
    data object Idle : PayPalCheckoutState
    data object CreatingOrder : PayPalCheckoutState
    data class AwaitingApproval(
        val paypalOrderId: String,
        val approvalStarted: Boolean = false
    ) : PayPalCheckoutState
    data object Capturing : PayPalCheckoutState
    data class Success(
        val message: String,
        val saleId: Int
    ) : PayPalCheckoutState
    data class Error(val message: String) : PayPalCheckoutState
    data object Canceled : PayPalCheckoutState
}

class CartViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _saleId = MutableStateFlow<Int?>(null)
    val saleId = _saleId.asStateFlow()

    private val _payPalState =
        MutableStateFlow<PayPalCheckoutState>(PayPalCheckoutState.Idle)
    val payPalState = _payPalState.asStateFlow()

    private var payPalToken: String? = null
    private var currentPayPalOrderId: String? = null

    fun addToCart(product: EcommerceProduct) {
        _items.update { current ->
            val index = current.indexOfFirst { it.product.id == product.id }

            if (index >= 0) {
                current.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) {
                        item.copy(quantity = item.quantity + 1)
                    } else {
                        item
                    }
                }
            } else {
                current + CartItem(product, 1)
            }
        }
    }

    fun removeItem(productId: Int) {
        _items.value = _items.value.filter { it.product.id != productId }
    }

    fun increaseQuantity(productId: Int) {
        _items.update { current ->
            current.map { item ->
                if (item.product.id == productId) {
                    item.copy(quantity = item.quantity + 1)
                } else {
                    item
                }
            }
        }
    }

    fun decreaseQuantity(productId: Int) {
        _items.update { current ->
            current.mapNotNull { item ->
                if (item.product.id == productId) {
                    val nextQuantity = item.quantity - 1
                    if (nextQuantity > 0) {
                        item.copy(quantity = nextQuantity)
                    } else {
                        null
                    }
                } else {
                    item
                }
            }
        }
    }

    fun totalPrice(): Int {
        return calculateTotal(_items.value)
    }

    private fun calculateTotal(items: List<CartItem>): Int {
        return items.sumOf {
            it.product.priceEur * it.quantity
        }
    }

    fun confirmManualOrder(token: String) {
        if (_items.value.isEmpty()) {
            _message.value = "El carrito está vacío"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _message.value = null

            try {
                val currentItems = _items.value
                val request = CheckoutRequest(
                    items = currentItems.map {
                        CheckoutItemRequest(
                            product_id = it.product.id,
                            quantity = it.quantity,
                            price = it.product.priceEur.toDouble()
                        )
                    },
                    total = calculateTotal(currentItems).toDouble(),
                    method_payment = "MOBILE_MANUAL"
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
            } catch (exception: HttpException) {
                val errorBody = exception.response()?.errorBody()?.string()
                Log.e(
                    MANUAL_CHECKOUT_TAG,
                    "HTTP ${exception.code()} en /api/mobile/checkout. Body: $errorBody",
                    exception
                )
                _message.value =
                    "Error HTTP ${exception.code()} al confirmar el pedido. Revisa Logcat."
                _saleId.value = null
            } catch (exception: Exception) {
                Log.e(
                    MANUAL_CHECKOUT_TAG,
                    "Respuesta no válida de /api/mobile/checkout: ${exception.message}",
                    exception
                )
                _message.value =
                    "Respuesta no válida del servidor. Revisa Logcat ($MANUAL_CHECKOUT_TAG)."
                _saleId.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun createPayPalOrder(token: String) {
        if (_items.value.isEmpty()) {
            _payPalState.value = PayPalCheckoutState.Error("El carrito está vacío")
            return
        }

        viewModelScope.launch {
            _payPalState.value = PayPalCheckoutState.CreatingOrder
            _message.value = null
            _saleId.value = null

            try {
                val request = MobilePayPalOrderRequest(
                    client_request_id = UUID.randomUUID().toString(),
                    items = _items.value.map {
                        MobilePayPalItemRequest(
                            product_id = it.product.id,
                            quantity = it.quantity
                        )
                    }
                )

                val response = ApiClient.api.createMobilePayPalOrder(
                    token = "Bearer $token",
                    body = request
                )

                if (response.message != 200 || response.paypal_order_id.isBlank()) {
                    _payPalState.value = PayPalCheckoutState.Error(
                        "Laravel no devolvió una orden PayPal válida."
                    )
                    return@launch
                }

                payPalToken = token
                currentPayPalOrderId = response.paypal_order_id
                _saleId.value = response.sale_id
                _payPalState.value = PayPalCheckoutState.AwaitingApproval(
                    paypalOrderId = response.paypal_order_id
                )
            } catch (exception: Exception) {
                _payPalState.value = PayPalCheckoutState.Error(
                    "No se pudo crear la orden PayPal: " +
                        (exception.message ?: "error desconocido")
                )
            }
        }
    }

    fun markPayPalApprovalStarted() {
        val state = _payPalState.value
        if (state is PayPalCheckoutState.AwaitingApproval) {
            _payPalState.value = state.copy(approvalStarted = true)
        }
    }

    fun capturePayPalOrder() {
        val token = payPalToken
        val paypalOrderId = currentPayPalOrderId

        if (token == null || paypalOrderId == null) {
            _payPalState.value = PayPalCheckoutState.Error(
                "No hay una orden PayPal pendiente de captura."
            )
            return
        }

        viewModelScope.launch {
            _payPalState.value = PayPalCheckoutState.Capturing

            try {
                val response = ApiClient.api.captureMobilePayPalOrder(
                    token = "Bearer $token",
                    paypalOrderId = paypalOrderId
                )

                if (response.message == 200 && response.status.equals("paid", ignoreCase = true)) {
                    _items.value = emptyList()
                    _saleId.value = response.sale_id
                    _payPalState.value = PayPalCheckoutState.Success(
                        message = response.message_text,
                        saleId = response.sale_id
                    )
                    payPalToken = null
                    currentPayPalOrderId = null
                } else {
                    _payPalState.value = PayPalCheckoutState.Error(
                        "La captura no se completó. El carrito se ha conservado."
                    )
                }
            } catch (exception: Exception) {
                _payPalState.value = PayPalCheckoutState.Error(
                    "No se pudo capturar el pago: " +
                        (exception.message ?: "error desconocido") +
                        ". El carrito se ha conservado."
                )
            }
        }
    }

    fun onPayPalCanceled() {
        _payPalState.value = PayPalCheckoutState.Canceled
        payPalToken = null
        currentPayPalOrderId = null
    }

    fun onPayPalApprovalFailed(detail: String?) {
        val suffix = detail?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        _payPalState.value = PayPalCheckoutState.Error(
            "PayPal no pudo completar la aprobación$suffix. El carrito se ha conservado."
        )
        payPalToken = null
        currentPayPalOrderId = null
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        private const val MANUAL_CHECKOUT_TAG = "ManualCheckout"
    }
}

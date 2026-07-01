package com.example.myapplication.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip


@Composable
fun CartScreen(
    vm: CartViewModel,
    token: String?,
    onBack: () -> Unit
) {
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()
    val saleId by vm.saleId.collectAsState()
    var selectedPaymentMethod by remember { mutableStateOf("MOBILE_MANUAL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("🛒 Carrito", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = if (it.contains("correctamente")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            saleId?.let { id ->
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Nº de pedido: $id",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (items.isEmpty()) {
            // 🟦 Carrito vacío
            Text("El carrito está vacío")

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (saleId != null) "Volver a productos" else "Volver"
                )
            }
        } else {
            // 🟩 Carrito con productos
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onIncrease = { vm.increaseQuantity(cartItem.product.id) },
                        onDecrease = { vm.decreaseQuantity(cartItem.product.id) },
                        onRemove = { vm.removeItem(cartItem.product.id) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Total: ${vm.totalPrice()} €",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Método de pago",
                style = MaterialTheme.typography.titleMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedPaymentMethod == "MOBILE_MANUAL",
                    onClick = { selectedPaymentMethod = "MOBILE_MANUAL" }
                )
                Text("Pago pendiente")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedPaymentMethod == "PAYPAL",
                    onClick = { selectedPaymentMethod = "PAYPAL" }
                )
                Text("PayPal Sandbox (pedido pendiente)")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = false,
                    onClick = null,
                    enabled = false
                )
                Text(
                    text = "Tarjeta (Próximamente)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))



            Button(
                onClick = {
                    token?.let {
                        vm.confirmOrder(it, selectedPaymentMethod)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && items.isNotEmpty() && token != null
            ) {
                Text(
                    if (loading) "Confirmando..." else "Confirmar pedido"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val img = cartItem.product.imagen.replace("127.0.0.1", "10.0.2.2")

        AsyncImage(
            model = fixImageUrl(cartItem.product.imagen),
            contentDescription = cartItem.product.title,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(cartItem.product.title, style = MaterialTheme.typography.bodyLarge)
            Text("Precio: ${cartItem.product.priceEur} €")
            Text("Cantidad: ${cartItem.quantity}")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onIncrease, contentPadding = PaddingValues(4.dp)) {
                Text("+")
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onDecrease, contentPadding = PaddingValues(4.dp)) {
                Text("-")
            }
        }

        Spacer(Modifier.width(10.dp))

        Button(
            onClick = onRemove,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("X")
        }
    }
}
fun fixImageUrl(url: String?): String {
    if (url.isNullOrBlank()) return ""

    return url
        .replace("http://127.0.0.1:8000", "http://10.0.2.2:8000")
        .replace("http://localhost:8000", "http://10.0.2.2:8000")
        .replace("127.0.0.1", "10.0.2.2")
        .replace("localhost", "10.0.2.2")
}

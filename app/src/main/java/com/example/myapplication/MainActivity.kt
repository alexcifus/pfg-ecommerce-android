package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.ui.cart.CartScreen
import com.example.myapplication.ui.cart.CartViewModel
import com.example.myapplication.ui.product.ProductViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.Alignment
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MyApplicationTheme {

                // ViewModels compartidos
                val authVM: AuthViewModel = viewModel()
                val productVM: ProductViewModel = viewModel()
                val cartVM: CartViewModel = viewModel()

                val user by authVM.user.collectAsState()

                if (user == null) {
                    LoginScreen(vm = authVM)
                } else {
                    ProductListScreen(
                        productVM = productVM,
                        cartVM = cartVM,
                        token = authVM.token
                    )
                }
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////
// LOGIN SCREEN
////////////////////////////////////////////////////////////////////////////////////

@Composable
fun LoginScreen(vm: AuthViewModel) {

    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val user by vm.user.collectAsState()

    var email by remember { mutableStateOf("alexcifu@gmail.com") }
    var password by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {

            Text("Login móvil", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.login(email, password) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Entrando..." else "Entrar")
            }

            Spacer(Modifier.height(16.dp))

            error?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error
                )
            }

            user?.let {
                Text("Bienvenido, ${it.name}")
                Text("Token: ${vm.token}")
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////
// LISTA DE PRODUCTOS + DETALLE + CARRITO
////////////////////////////////////////////////////////////////////////////////////

@Composable
fun ProductListScreen(
    productVM: ProductViewModel,
    cartVM: CartViewModel,
    token: String?
) {
    val loading by productVM.loading.collectAsState()
    val error by productVM.error.collectAsState()
    val products by productVM.products.collectAsState()

    // Estados de navegación internos
    var selectedProduct by remember { mutableStateOf<EcommerceProduct?>(null) }
    var showCart by remember { mutableStateOf(false) }

    // Cargar productos al entrar
    LaunchedEffect(Unit) {
        productVM.loadProducts()
    }

    Surface(Modifier.fillMaxSize()) {
        // Usamos un Box para poder alinear el FAB en la esquina
        Box(Modifier.fillMaxSize()) {

            when {
                showCart -> {
                    // Pantalla de carrito
                    CartScreen(
                        vm = cartVM,
                        token = token,
                        onBack = {
                            showCart = false
                        }
                    )
                }

                selectedProduct != null -> {
                    // Pantalla de detalle
                    ProductDetailScreen(
                        product = selectedProduct!!,
                        onBack = { selectedProduct = null },
                        onAddToCart = { cartVM.addToCart(selectedProduct!!) }
                    )
                }

                else -> {
                    // Lista de productos
                    Column(Modifier.padding(16.dp)) {
                        Text("Productos", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(16.dp))

                        if (loading) {
                            Text("Cargando productos...")
                        }

                        error?.let {
                            Text(
                                text = "Error: $it",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        LazyColumn {
                            items(products) { product ->
                                ProductItemRow(
                                    product = product,
                                    onClick = { selectedProduct = product }
                                )
                            }
                        }
                    }
                }
            }

            // Botón flotante de carrito (siempre visible sobre el contenido)
            FloatingActionButton(
                onClick = { showCart = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Carrito"
                )
            }
        }
    }
}


////////////////////////////////////////////////////////////////////////////////////
// PRODUCT ITEM (ROW)
////////////////////////////////////////////////////////////////////////////////////

@Composable
fun ProductItemRow(
    product: EcommerceProduct,
    onClick: () -> Unit
) {
    val img = product.imagen.replace("127.0.0.1", "10.0.2.2")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = fixImageUrl(product.imagen),
            contentDescription = product.title,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column {
            Text(product.title, style = MaterialTheme.typography.bodyLarge)
            Text("Precio: ${product.priceEur} €")
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////
// PRODUCT DETAIL SCREEN
////////////////////////////////////////////////////////////////////////////////////

@Composable
fun ProductDetailScreen(
    product: EcommerceProduct,
    onBack: () -> Unit,
    onAddToCart: () -> Unit
) {
    val img = product.imagen.replace("127.0.0.1", "10.0.2.2")

    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            "← Volver",
            modifier = Modifier.clickable { onBack() },
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        AsyncImage(
            model = fixImageUrl(product.imagen),
            contentDescription = product.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(product.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Precio: ${product.priceEur} €")

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                onAddToCart()
                Toast.makeText(
                    context,
                    "Producto añadido al carrito",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir al carrito")
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

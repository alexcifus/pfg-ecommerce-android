package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.example.myapplication.R
import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.network.ImageUrlNormalizer
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
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest



class MainActivity : ComponentActivity() {

    private val cartViewModel: CartViewModel by viewModels()
    private var payPalWebCheckoutClient: PayPalWebCheckoutClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurePayPal()
        savedInstanceState?.getString(PAYPAL_CLIENT_STATE)?.let {
            payPalWebCheckoutClient?.restore(it)
        }
        handlePayPalReturn(intent)

        setContent {

            MyApplicationTheme {

                // ViewModels compartidos
                val authVM: AuthViewModel = viewModel()
                val productVM: ProductViewModel = viewModel()
                val user by authVM.user.collectAsState()

                if (user == null) {
                    LoginScreen(vm = authVM)
                } else {
                    ProductListScreen(
                        productVM = productVM,
                        cartVM = cartViewModel,
                        token = authVM.token,
                        onPayPalApprovalRequested = ::startPayPalApproval
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePayPalReturn(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        payPalWebCheckoutClient?.instanceState?.let {
            outState.putString(PAYPAL_CLIENT_STATE, it)
        }
        super.onSaveInstanceState(outState)
    }

    private fun configurePayPal() {
        if (BuildConfig.PAYPAL_CLIENT_ID.isBlank()) {
            return
        }

        val coreConfig = CoreConfig(
            clientId = BuildConfig.PAYPAL_CLIENT_ID,
            environment = Environment.SANDBOX
        )
        payPalWebCheckoutClient = PayPalWebCheckoutClient(
            this,
            coreConfig,
            PAYPAL_RETURN_URL_SCHEME
        )
    }

    private fun startPayPalApproval(paypalOrderId: String) {
        val client = payPalWebCheckoutClient
        if (client == null) {
            cartViewModel.onPayPalApprovalFailed(
                "Configura PAYPAL_CLIENT_ID en local.properties"
            )
            return
        }

        val request = PayPalWebCheckoutRequest(
            orderId = paypalOrderId,
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )
        client.start(this, request) { result ->
            if (result is PayPalPresentAuthChallengeResult.Failure) {
                cartViewModel.onPayPalApprovalFailed(result.error.errorDescription)
            }
        }
    }

    private fun handlePayPalReturn(intent: Intent) {
        val client = payPalWebCheckoutClient ?: return

        when (val result = client.finishStart(intent)) {
            is PayPalWebCheckoutFinishStartResult.Success ->
                cartViewModel.capturePayPalOrder()
            is PayPalWebCheckoutFinishStartResult.Canceled ->
                cartViewModel.onPayPalCanceled()
            is PayPalWebCheckoutFinishStartResult.Failure ->
                cartViewModel.onPayPalApprovalFailed(result.error.errorDescription)
            PayPalWebCheckoutFinishStartResult.NoResult, null -> Unit
        }
    }

    companion object {
        private const val PAYPAL_CLIENT_STATE = "paypal_client_state"
        private const val PAYPAL_RETURN_URL_SCHEME = "com.example.myapplication.paypal"
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
    token: String?,
    onPayPalApprovalRequested: (String) -> Unit
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
                        onPayPalApprovalRequested = onPayPalApprovalRequested,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUrlNormalizer.normalize(product.imagen),
            contentDescription = product.title,
            placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_product_placeholder),
            error = androidx.compose.ui.res.painterResource(R.drawable.ic_product_placeholder),
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
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            "← Volver",
            modifier = Modifier.clickable { onBack() },
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        AsyncImage(
            model = ImageUrlNormalizer.normalize(product.imagen),
            contentDescription = product.title,
            placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_product_placeholder),
            error = androidx.compose.ui.res.painterResource(R.drawable.ic_product_placeholder),
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

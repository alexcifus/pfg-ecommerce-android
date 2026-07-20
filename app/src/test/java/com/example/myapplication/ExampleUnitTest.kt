package com.example.myapplication

import com.example.myapplication.model.EcommerceProduct
import com.example.myapplication.ui.cart.CartViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun emptyCartTotalIsZero() {
        val vm = CartViewModel()

        assertEquals(0, vm.totalPrice())
    }

    @Test
    fun productFortyByOneIsForty() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))

        assertEquals(40, vm.totalPrice())
        assertEquals(1, vm.items.value.single().quantity)
    }

    @Test
    fun productFortyByTwoIsEighty() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))
        vm.addToCart(product(id = 1, priceEur = 40))

        assertEquals(80, vm.totalPrice())
        assertEquals(2, vm.items.value.single().quantity)
    }

    @Test
    fun productFortyByThreeIsOneHundredTwenty() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))
        vm.addToCart(product(id = 1, priceEur = 40))
        vm.addToCart(product(id = 1, priceEur = 40))

        assertEquals(120, vm.totalPrice())
        assertEquals(3, vm.items.value.single().quantity)
    }

    @Test
    fun twoDifferentProductsTotalIsSumOfEachLine() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))
        vm.addToCart(product(id = 2, priceEur = 25))
        vm.addToCart(product(id = 2, priceEur = 25))

        assertEquals(90, vm.totalPrice())
    }

    @Test
    fun increaseAndDecreaseUpdateTotal() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))
        vm.increaseQuantity(productId = 1)
        assertEquals(80, vm.totalPrice())
        assertEquals(2, vm.items.value.single().quantity)

        vm.decreaseQuantity(productId = 1)
        assertEquals(40, vm.totalPrice())
        assertEquals(1, vm.items.value.single().quantity)
    }

    @Test
    fun quantityChangesEmitUpdatedCartState() = runBlocking {
        val vm = CartViewModel()
        val emittedQuantities = mutableListOf<Int>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            vm.items
                .drop(1)
                .take(3)
                .map { items -> items.single().quantity }
                .toList(emittedQuantities)
        }

        vm.addToCart(product(id = 1, priceEur = 40))
        yield()
        vm.increaseQuantity(productId = 1)
        yield()
        vm.decreaseQuantity(productId = 1)
        yield()

        withTimeout(1_000) {
            job.join()
        }

        assertEquals(listOf(1, 2, 1), emittedQuantities)
    }

    @Test
    fun removeItemLeavesCorrectTotal() {
        val vm = CartViewModel()

        vm.addToCart(product(id = 1, priceEur = 40))
        vm.addToCart(product(id = 2, priceEur = 25))
        vm.addToCart(product(id = 2, priceEur = 25))
        vm.removeItem(productId = 2)

        assertEquals(40, vm.totalPrice())
        assertEquals(1, vm.items.value.single().product.id)
    }

    private fun product(id: Int, priceEur: Int): EcommerceProduct {
        return EcommerceProduct(
            id = id,
            title = "Product $id",
            priceEur = priceEur,
            imagen = ""
        )
    }
}

package com.focx.data.datasource.mock


import com.focx.domain.entity.Product
import com.focx.domain.repository.IProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockProductDataSource : IProductRepository {

    private var products: MutableList<Product> = mockProducts.toMutableList()

    override suspend fun getProducts(page: Int, pageSize: Int, refresh: Boolean): Flow<List<Product>> = flow {
        delay(500) // Simulate network delay
        if (refresh) {
            products = mockProducts.shuffled().toMutableList()
        }
        val start = (page - 1) * pageSize
        val end = minOf(start + pageSize, products.size)
        if (start >= products.size) {
            emit(emptyList())
        } else {
            emit(products.subList(start, end))
        }
    }

    override suspend fun getProductById(productId: ULong): Flow<Product?> = flow {
        delay(500)
        emit(mockProducts.find { it.id == productId })
    }

    override suspend fun searchProducts(query: String, page: Int, pageSize: Int): Flow<List<Product>> =
        flow {
            delay(500)
            val filtered = mockProducts.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
            val start = (page - 1) * pageSize
            val end = minOf(start + pageSize, filtered.size)
            if (start >= filtered.size) {
                emit(emptyList())
            } else {
                emit(filtered.subList(start, end))
            }
        }

    override suspend fun getProductsByCategory(
        category: String,
        page: Int,
        pageSize: Int
    ): Flow<List<Product>> = flow {
        delay(500)
        val filtered = mockProducts.filter { it.category == category }
        val start = (page - 1) * pageSize
        val end = minOf(start + pageSize, filtered.size)
        if (start >= filtered.size) {
            emit(emptyList())
        } else {
            emit(filtered.subList(start, end))
        }
    }

    override suspend fun getRelatedProducts(productId: ULong, count: Int): Flow<List<Product>> = flow {
        delay(500)
        val product = mockProducts.find { it.id == productId }
        if (product != null) {
            val related = mockProducts.filter {
                it.category == product.category && it.id != productId
            }.take(count)
            emit(related)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun saveProduct(product: Product, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        delay(500)
        // In mock implementation, we ignore the accountPublicKey and activityResultSender
        products.add(product)
    }

    override suspend fun updateProduct(product: Product, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        delay(500)
        // In mock implementation, we ignore the accountPublicKey and activityResultSender
        val index = products.indexOfFirst { it.id == product.id }
        if (index != -1) {
            products[index] = product
        }
    }

    override suspend fun deleteProduct(productId: ULong, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        delay(500)
        // In mock implementation, we ignore the accountPublicKey and activityResultSender
        products.removeAll { it.id == productId }
    }

}
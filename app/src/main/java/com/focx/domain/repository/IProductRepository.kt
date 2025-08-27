package com.focx.domain.repository

import com.focx.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface IProductRepository {
    suspend fun getProducts(page: Int, pageSize: Int, refresh: Boolean = false): Flow<List<Product>>
    suspend fun getProductById(productId: ULong): Flow<Product?>
    suspend fun searchProducts(query: String, page: Int, pageSize: Int): Flow<List<Product>>
    suspend fun getProductsByCategory(category: String, page: Int, pageSize: Int): Flow<List<Product>>
    suspend fun getRelatedProducts(productId: ULong, count: Int): Flow<List<Product>>
    suspend fun saveProduct(product: Product, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender)
    suspend fun updateProduct(product: Product, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender)
    suspend fun deleteProduct(productId: ULong, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender)
    
    fun clearRecommendCache()
    fun hasValidRecommendCache(): Boolean
}
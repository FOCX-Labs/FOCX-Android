package com.focx.domain.repository

import com.focx.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface IProductRepository {
    suspend fun getProducts(page: Int, pageSize: Int, refresh: Boolean = false): Flow<List<Product>>
    suspend fun getProductById(productId: String): Flow<Product?>
    suspend fun searchProducts(query: String, page: Int, pageSize: Int): Flow<List<Product>>
    suspend fun getProductsByCategory(category: String, page: Int, pageSize: Int): Flow<List<Product>>
    suspend fun getRelatedProducts(productId: String, count: Int): Flow<List<Product>>
}
package com.focx.domain.usecase

import com.focx.domain.entity.Product
import com.focx.domain.repository.IProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(page: Int, pageSize: Int, refresh: Boolean = false): Flow<Result<List<Product>>> {
        return productRepository.getProducts(page, pageSize, refresh)
            .map { products ->
                Result.success(products)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetProductByIdUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(productId: String): Flow<Result<Product?>> {
        return productRepository.getProductById(productId)
            .map { product ->
                Result.success(product)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class SearchProductsUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(query: String, page: Int, pageSize: Int): Flow<Result<List<Product>>> {
        return productRepository.searchProducts(query, page, pageSize)
            .map { products ->
                Result.success(products)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}
package com.focx.domain.usecase

import com.focx.domain.repository.IProductRepository
import javax.inject.Inject

class CacheManagementUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    fun clearRecommendCache() {
        productRepository.clearRecommendCache()
    }

    fun hasValidRecommendCache(): Boolean {
        return productRepository.hasValidRecommendCache()
    }
}

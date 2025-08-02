package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaProductDataSource
import com.focx.domain.entity.Product
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMerchantProductsUseCase @Inject constructor(
    private val productDataSource: SolanaProductDataSource
) {
    suspend operator fun invoke(merchantAddress: String): Flow<Result<List<Product>>> {
        return productDataSource.getMerchantProducts(merchantAddress)
    }
} 
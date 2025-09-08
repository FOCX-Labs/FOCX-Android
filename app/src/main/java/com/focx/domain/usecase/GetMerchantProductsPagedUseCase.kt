package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaProductDataSource
import com.focx.domain.entity.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMerchantProductsPagedUseCase @Inject constructor(
    private val productDataSource: SolanaProductDataSource
) {
    suspend operator fun invoke(merchantAddress: String, page: Int, pageSize: Int): Flow<Result<List<Product>>> {
        return productDataSource.getMerchantProductsPaged(merchantAddress, page, pageSize)
            .map { result ->
                result.fold(
                    onSuccess = { products -> Result.success(products) },
                    onFailure = { exception -> Result.failure(exception) }
                )
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}
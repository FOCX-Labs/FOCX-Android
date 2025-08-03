package com.focx.domain.usecase

import com.focx.domain.repository.IProductRepository
import javax.inject.Inject

class DeleteProductUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(productId: ULong, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        productRepository.deleteProduct(productId, accountPublicKey, activityResultSender)
    }
} 
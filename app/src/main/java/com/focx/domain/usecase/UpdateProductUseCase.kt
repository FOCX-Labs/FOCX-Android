package com.focx.domain.usecase

import com.focx.domain.entity.Product
import com.focx.domain.repository.IProductRepository
import javax.inject.Inject

class UpdateProductUseCase @Inject constructor(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(product: Product, accountPublicKey: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        productRepository.updateProduct(product, accountPublicKey, activityResultSender)
    }
}
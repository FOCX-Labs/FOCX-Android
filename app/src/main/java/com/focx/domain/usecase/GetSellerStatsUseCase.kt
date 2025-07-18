package com.focx.domain.usecase

import com.focx.domain.entity.SellerStats
import com.focx.domain.repository.ISellerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSellerStatsUseCase @Inject constructor(
    private val sellerRepository: ISellerRepository
) {
    operator fun invoke(sellerId: String): Flow<SellerStats> {
        return sellerRepository.getSellerStats(sellerId)
    }
}
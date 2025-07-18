package com.focx.data.datasource.mock

import com.focx.domain.entity.SellerStats
import com.focx.domain.repository.ISellerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSellerDataSource @Inject constructor() : ISellerRepository {

    override fun getSellerStats(sellerId: String): Flow<SellerStats> = flow {
        delay(500) // Simulate network delay
        emit(
            SellerStats(
                totalSales = "¥12,580",
                totalOrders = 45,
                totalProducts = 12,
                averageRating = 4.8f
            )
        )
    }
}
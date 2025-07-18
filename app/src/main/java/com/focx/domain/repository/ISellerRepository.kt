package com.focx.domain.repository

import com.focx.domain.entity.SellerStats
import kotlinx.coroutines.flow.Flow

interface ISellerRepository {
    fun getSellerStats(sellerId: String): Flow<SellerStats>
}
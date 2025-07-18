package com.focx.domain.entity

data class SellerStats(
    val totalSales: String,
    val totalOrders: Int,
    val totalProducts: Int,
    val averageRating: Float
)
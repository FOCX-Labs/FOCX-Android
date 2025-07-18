package com.focx.domain.entity

data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productImage: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)
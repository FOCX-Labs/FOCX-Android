package com.focx.domain.entity

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String = "USDC",
    val imageUrls: List<String>,
    val sellerId: String,
    val sellerName: String,
    val category: String,
    val stock: Int,
    val salesCount: Int = 0,
    val shippingFrom: String,
    val shippingTo: List<String>,
    val shippingMethods: List<String>,
    val specifications: Map<String, String> = emptyMap(),
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
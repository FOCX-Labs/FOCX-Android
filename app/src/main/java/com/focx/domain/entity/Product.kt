package com.focx.domain.entity

import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

data class Product(
    val id: ULong,
    val name: String,
    val description: String,
    val price: ULong,
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
)

@Serializable
data class CreateProductBase(
    val productName: String,
    val productDescription: String,
    val price: ULong,
    val keywords: List<String>,
    val inventory: ULong,
    val paymentToken: SolanaPublicKey,
    val shippingLocation: String
)

@Serializable
data class CreateProductExtended(
    val productId: ULong,
    val imageVideoUrls: List<String>,
    val salesRegions: List<String>,
    val logisticsMethods: List<String>
)

@Serializable
data class AddProductToKeywordIndex(
    val keyword: String,
    val productId: ULong
)

@Serializable
data class AddProductToPriceIndex(
    val priceRangeStart: ULong,
    val priceRangeEnd: ULong,
    val productId: ULong,
    val price: ULong
)

@Serializable
data class AddProductToSalesIndex(
    val salesRangeStart: UInt,
    val salesRangeEnd: UInt,
    val productId: ULong,
    val sales: UInt
)
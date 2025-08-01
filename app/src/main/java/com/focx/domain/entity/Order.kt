package com.focx.domain.entity

import kotlinx.serialization.Serializable

data class Order(
    val id: String,
    val buyerId: String,
    val sellerId: String,
    val sellerName: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val currency: String = "USDC",
    val status: OrderManagementStatus,
    val shippingAddress: ShippingAddress? = null,
    val orderNote: String? = null,
    val paymentMethod: String,
    val transactionHash: String? = null,
    val trackingNumber: String? = null,
    val orderDate: Long = System.currentTimeMillis() / 1000,
    val updatedAt: Long = System.currentTimeMillis() / 1000,
    val estimatedDelivery: Long? = null
)

enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}

data class ShippingAddress(
    val recipientName: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val phoneNumber: String
)

@Serializable
data class CreateOrder(
    val productId: ULong,
    val quantity: UInt,
    val shippingAddress: String,
    val notes: String,
    val transactionSignature: String
)

@Serializable
data class OrderPayment(
    val productId: ULong,
    val quantity: ULong,
)
package com.focx.domain.repository

import com.focx.domain.entity.Order
import com.focx.domain.entity.Product
import com.focx.presentation.intent.ProductDetailIntent
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.flow.Flow

interface IOrderRepository {
    suspend fun getOrders(): Flow<List<Order>>
    suspend fun getOrderById(id: String): Order?
    suspend fun getOrdersByBuyer(buyerId: String): Flow<List<Order>>
    suspend fun getOrdersBySeller(sellerId: String): Flow<List<Order>>
    suspend fun getOrdersByStatus(status: String): Flow<List<Order>>
    suspend fun createOrder(product: Product, quantity: UInt, buyer: String, activityResultSender: ActivityResultSender): Result<Order>
    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun updateTrackingNumber(orderId: String, trackingNumber: String): Result<Unit>
}
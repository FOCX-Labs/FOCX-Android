package com.focx.domain.repository

import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.entity.Product
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.flow.Flow

interface IOrderRepository {
    suspend fun getOrders(): Flow<List<Order>>
    suspend fun getOrderById(id: String): Order?
    suspend fun getOrdersByBuyer(buyerId: String): Flow<List<Order>>
    suspend fun getOrdersBySeller(sellerId: String): Flow<List<Order>>
    suspend fun getOrdersByStatus(status: OrderManagementStatus): Flow<List<Order>>
    suspend fun createOrder(product: Product, quantity: UInt, buyer: String, activityResultSender: ActivityResultSender): Result<Order>
    suspend fun updateOrderStatus(orderId: String, status: OrderManagementStatus): Result<Unit>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun updateTrackingNumber(orderId: String, trackingNumber: String, merchantPubKey: SolanaPublicKey, activityResultSender: ActivityResultSender): Result<Unit>
    suspend fun confirmReceipt(orderId: String, buyerPubKey: SolanaPublicKey, merchantPubKey: SolanaPublicKey, activityResultSender: ActivityResultSender): Result<Unit>
}
package com.focx.domain.usecase

import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.repository.IOrderRepository
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetOrdersUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Order>>> {
        return orderRepository.getOrders()
            .map { orders ->
                Result.success(orders)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetOrdersByBuyerUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(buyerId: String): Flow<Result<List<Order>>> {
        return orderRepository.getOrdersByBuyer(buyerId)
            .map { orders ->
                Result.success(orders)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetOrdersBySellerUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(sellerId: String): Flow<Result<List<Order>>> {
        return orderRepository.getOrdersBySeller(sellerId)
            .map { orders ->
                Result.success(orders)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetOrdersByStatusUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(status: OrderManagementStatus): Flow<Result<List<Order>>> {
        return orderRepository.getOrdersByStatus(status)
            .map { orders ->
                Result.success(orders)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class CreateOrderUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(product: com.focx.domain.entity.Product, quantity: UInt, buyer: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Order> {
        return orderRepository.createOrder(product, quantity, buyer, activityResultSender)
    }
}

class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(orderId: String, status: OrderManagementStatus): Result<Unit> {
        return orderRepository.updateOrderStatus(orderId, status)
    }
}

class UpdateTrackingNumberUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(orderId: String, trackingNumber: String, merchantPubKey: SolanaPublicKey, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Unit> {
        return orderRepository.updateTrackingNumber(orderId, trackingNumber, merchantPubKey, activityResultSender)
    }
}

class ConfirmReceiptUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(orderId: String,
                                buyerPubKey: SolanaPublicKey,
                                merchantPubKey: SolanaPublicKey,
                                activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Unit> {
        return orderRepository.confirmReceipt(orderId, buyerPubKey, merchantPubKey, activityResultSender)
    }
}
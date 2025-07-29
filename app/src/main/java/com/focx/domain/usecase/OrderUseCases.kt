package com.focx.domain.usecase

import com.focx.domain.entity.Order
import com.focx.domain.repository.IOrderRepository
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
    suspend operator fun invoke(orderId: String, status: String): Result<Unit> {
        return orderRepository.updateOrderStatus(orderId, status)
    }
}
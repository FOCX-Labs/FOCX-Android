package com.focx.domain.usecase

import com.focx.domain.entity.Order
import com.focx.domain.repository.IOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetOrdersBySellerPagedUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(sellerId: String, page: Int, pageSize: Int): Flow<Result<List<Order>>> {
        return orderRepository.getOrdersBySellerPaged(sellerId, page, pageSize)
            .map { orders ->
                Result.success(orders)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}
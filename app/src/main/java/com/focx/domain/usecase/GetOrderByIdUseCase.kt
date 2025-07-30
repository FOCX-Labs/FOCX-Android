package com.focx.domain.usecase

import com.focx.domain.entity.Order
import com.focx.domain.repository.IOrderRepository
import javax.inject.Inject

class GetOrderByIdUseCase @Inject constructor(
    private val orderRepository: IOrderRepository
) {
    suspend operator fun invoke(orderId: String): Order? {
        return orderRepository.getOrderById(orderId)
    }
}
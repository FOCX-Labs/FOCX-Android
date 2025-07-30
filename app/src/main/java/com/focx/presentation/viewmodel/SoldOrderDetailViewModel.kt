package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.usecase.GetOrderByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SoldOrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OrderStatusStep(
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@HiltViewModel
class SoldOrderDetailViewModel @Inject constructor(
    private val getOrderByIdUseCase: GetOrderByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SoldOrderDetailState())
    val state: StateFlow<SoldOrderDetailState> = _state.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val order = getOrderByIdUseCase(orderId)
                _state.value = _state.value.copy(
                    order = order,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load order",
                    isLoading = false
                )
            }
        }
    }

    fun getOrderStatusSteps(order: Order): List<OrderStatusStep> {
        val steps = mutableListOf<OrderStatusStep>()
        
        // Always add "Order Placed" as the first step
        steps.add(
            OrderStatusStep(
                title = "Order Placed",
                description = "Customer placed the order",
                timestamp = formatTimestamp(order.orderDate),
                isCompleted = true,
                isCurrent = order.status == OrderManagementStatus.Pending
            )
        )
        
        // Add "Payment Confirmed" if payment is processed
        if (order.transactionHash != null) {
            steps.add(
                OrderStatusStep(
                    title = "Payment Confirmed",
                    description = "Payment successfully processed",
                    timestamp = formatTimestamp(order.orderDate + 60000), // 1 minute after order
                    isCompleted = true,
                    isCurrent = order.status == OrderManagementStatus.Pending
                )
            )
        }
        
        // Add status-specific steps
        when (order.status) {
            OrderManagementStatus.Pending -> {
                steps.add(
                    OrderStatusStep(
                        title = "Processing",
                        description = "Order is being prepared",
                        timestamp = formatTimestamp(order.updatedAt),
                        isCompleted = true,
                        isCurrent = true
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Shipped",
                        description = "Package will be shipped soon",
                        timestamp = "Pending",
                        isCompleted = false,
                        isCurrent = false
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Delivered",
                        description = "Package will be delivered",
                        timestamp = order.estimatedDelivery?.let { "Estimated: ${formatDate(it)}" } ?: "TBD",
                        isCompleted = false,
                        isCurrent = false
                    )
                )
            }
            OrderManagementStatus.Shipped -> {
                steps.add(
                    OrderStatusStep(
                        title = "Processing",
                        description = "Order was prepared",
                        timestamp = formatTimestamp(order.updatedAt - 86400000), // 1 day before shipped
                        isCompleted = true,
                        isCurrent = false
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Shipped",
                        description = "Package has been shipped",
                        timestamp = formatTimestamp(order.updatedAt),
                        isCompleted = true,
                        isCurrent = true
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Delivered",
                        description = "Package will be delivered",
                        timestamp = order.estimatedDelivery?.let { "Estimated: ${formatDate(it)}" } ?: "TBD",
                        isCompleted = false,
                        isCurrent = false
                    )
                )
            }
            OrderManagementStatus.Delivered -> {
                steps.add(
                    OrderStatusStep(
                        title = "Processing",
                        description = "Order was prepared",
                        timestamp = formatTimestamp(order.orderDate + 3600000), // 1 hour after order
                        isCompleted = true,
                        isCurrent = false
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Shipped",
                        description = "Package was shipped",
                        timestamp = formatTimestamp(order.updatedAt - 86400000), // 1 day before delivered
                        isCompleted = true,
                        isCurrent = false
                    )
                )
                steps.add(
                    OrderStatusStep(
                        title = "Delivered",
                        description = "Package has been delivered",
                        timestamp = formatTimestamp(order.updatedAt),
                        isCompleted = true,
                        isCurrent = true
                    )
                )
            }
            OrderManagementStatus.Refunded -> {
                steps.add(
                    OrderStatusStep(
                        title = "Refunded",
                        description = "Order has been refunded",
                        timestamp = formatTimestamp(order.updatedAt),
                        isCompleted = true,
                        isCurrent = true
                    )
                )
            }
        }
        
        return steps
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
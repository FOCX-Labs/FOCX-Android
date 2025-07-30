package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderItem
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.entity.ShippingAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val createOrderUseCase: com.focx.domain.usecase.CreateOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    @Inject
    lateinit var getCurrentWalletAddressUseCase: com.focx.domain.usecase.GetCurrentWalletAddressUseCase

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // TODO: Replace with actual repository call
                val mockOrders = generateMockOrders()
                _uiState.value = _uiState.value.copy(
                    orders = mockOrders,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // TODO: Replace with actual repository call
                // For now, just use the existing orders or generate mock data
                if (_uiState.value.orders.isEmpty()) {
                    val mockOrders = generateMockOrders()
                    _uiState.value = _uiState.value.copy(
                        orders = mockOrders,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun buyProduct(
        product: com.focx.domain.entity.Product,
        quantity: UInt,
        activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
        onResult: (Result<Order>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val buyer = getCurrentWalletAddressUseCase.execute()!!
                val result = createOrderUseCase(product, quantity, buyer, activityResultSender)
                onResult(result)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                onResult(Result.failure(e))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun generateMockOrders(): List<Order> {
        return listOf(
            Order(
                id = "ORD001",
                buyerId = "USER001",
                sellerId = "SELLER001",
                sellerName = "TechStore",
                items = listOf(
                    OrderItem(
                        id = "ITEM001",
                        productId = "PROD001",
                        productName = "Wireless Bluetooth Headphones",
                        productImage = "https://example.com/headphones.jpg",
                        quantity = 1,
                        unitPrice = 249.99,
                        totalPrice = 249.99
                    )
                ),
                totalAmount = 259.98,
                status = OrderManagementStatus.Delivered,
                shippingAddress = ShippingAddress(
                    recipientName = "John Doe",
                    addressLine1 = "123 Main Street",
                    addressLine2 = "Apt 4B",
                    city = "New York",
                    state = "NY",
                    postalCode = "10001",
                    country = "United States",
                    phoneNumber = "+1 (555) 123-4567"
                ),
                paymentMethod = "USDC",
                transactionHash = "0x1234567890abcdef",
                trackingNumber = "TRK001",
                orderDate = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000),
                estimatedDelivery = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000)
            ),
            Order(
                id = "ORD002",
                buyerId = "USER001",
                sellerId = "SELLER002",
                sellerName = "WearableTech",
                items = listOf(
                    OrderItem(
                        id = "ITEM002",
                        productId = "PROD002",
                        productName = "Smart Watch",
                        productImage = "https://example.com/smartwatch.jpg",
                        quantity = 1,
                        unitPrice = 129.99,
                        totalPrice = 129.99
                    )
                ),
                totalAmount = 135.98,
                status = OrderManagementStatus.Shipped,
                shippingAddress = ShippingAddress(
                    recipientName = "John Doe",
                    addressLine1 = "123 Main Street",
                    addressLine2 = "Apt 4B",
                    city = "New York",
                    state = "NY",
                    postalCode = "10001",
                    country = "United States",
                    phoneNumber = "+1 (555) 123-4567"
                ),
                paymentMethod = "USDC",
                transactionHash = "0xabcdef1234567890",
                trackingNumber = "TRK002",
                orderDate = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000),
                estimatedDelivery = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000)
            ),
            Order(
                id = "ORD003",
                buyerId = "USER001",
                sellerId = "SELLER003",
                sellerName = "CableWorld",
                items = listOf(
                    OrderItem(
                        id = "ITEM003",
                        productId = "PROD003",
                        productName = "USB Cable",
                        productImage = "https://example.com/usbcable.jpg",
                        quantity = 3,
                        unitPrice = 19.99,
                        totalPrice = 59.97
                    )
                ),
                totalAmount = 64.96,
                status = OrderManagementStatus.Pending,
                shippingAddress = ShippingAddress(
                    recipientName = "John Doe",
                    addressLine1 = "456 Business Ave",
                    addressLine2 = "Suite 200",
                    city = "New York",
                    state = "NY",
                    postalCode = "10002",
                    country = "United States",
                    phoneNumber = "+1 (555) 987-6543"
                ),
                paymentMethod = "USDC",
                transactionHash = "0x567890abcdef1234",
                orderDate = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000),
                estimatedDelivery = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000)
            ),
            Order(
                id = "ORD004",
                buyerId = "USER001",
                sellerId = "SELLER004",
                sellerName = "OfficeSupplies",
                items = listOf(
                    OrderItem(
                        id = "ITEM004",
                        productId = "PROD004",
                        productName = "Laptop Stand",
                        productImage = "https://example.com/laptopstand.jpg",
                        quantity = 1,
                        unitPrice = 89.99,
                        totalPrice = 89.99
                    )
                ),
                totalAmount = 96.99,
                status = OrderManagementStatus.Refunded,
                shippingAddress = ShippingAddress(
                    recipientName = "John Doe",
                    addressLine1 = "123 Main Street",
                    addressLine2 = "Apt 4B",
                    city = "New York",
                    state = "NY",
                    postalCode = "10001",
                    country = "United States",
                    phoneNumber = "+1 (555) 123-4567"
                ),
                paymentMethod = "USDC",
                orderDate = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000),
                estimatedDelivery = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
            )
        )
    }
}
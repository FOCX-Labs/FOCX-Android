package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.*
import com.focx.domain.usecase.CreateOrderUseCase
import com.focx.domain.usecase.GetOrderByIdUseCase
import com.focx.domain.usecase.GetOrdersByBuyerPagedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject
import com.focx.utils.Log

data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMoreOrders: Boolean = true,
    val totalOrders: Int = 0
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val getOrdersByBuyerPagedUseCase: GetOrdersByBuyerPagedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    
    private val PAGE_SIZE = 5 // Number of orders to display per page

    @Inject
    lateinit var getCurrentWalletAddressUseCase: com.focx.domain.usecase.GetCurrentWalletAddressUseCase

    fun resetAndLoadOrders() {
        Log.d("OrderViewModel", "resetAndLoadOrders called")
        
        // Cancel any ongoing coroutines to prevent interference
        viewModelScope.coroutineContext.cancelChildren()
        
        // Force reset state completely
        _uiState.value = OrderUiState()
        
        // Add a small delay to ensure state is fully reset
        viewModelScope.launch {
            kotlinx.coroutines.delay(50) // Small delay to ensure state reset
            loadOrders()
        }
    }

    fun loadOrders(initialLoad: Int = PAGE_SIZE) {
        Log.d("OrderViewModel", "loadOrders called")
        
        viewModelScope.launch {
            // Reset state and start fresh load
            _uiState.value = OrderUiState(isLoading = true)
            
            try {
                val buyer = getCurrentWalletAddressUseCase.execute()!!
                getOrdersByBuyerPagedUseCase(buyer, 1, initialLoad)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }.collect { result ->
                        result.fold(
                            onSuccess = { orders ->
                                Log.d("OrderViewModel", "Initial orders fetched: ${orders.size}")
                                
                                _uiState.value = _uiState.value.copy(
                                    orders = orders,
                                    isLoading = false,
                                    error = null,
                                    currentPage = 1,
                                    hasMoreOrders = orders.size == initialLoad,
                                    totalOrders = orders.size
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load orders: ${e.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun loadMoreOrders() {
        val currentState = _uiState.value
        Log.d("OrderViewModel", "loadMoreOrders called - currentPage: ${currentState.currentPage}, hasMore: ${currentState.hasMoreOrders}, isLoadingMore: ${currentState.isLoadingMore}, ordersCount: ${currentState.orders.size}")
        
        if (currentState.isLoadingMore || !currentState.hasMoreOrders) {
            Log.d("OrderViewModel", "loadMoreOrders skipped - isLoadingMore: ${currentState.isLoadingMore}, hasMoreOrders: ${currentState.hasMoreOrders}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val buyer = getCurrentWalletAddressUseCase.execute()!!
                val nextPage = currentState.currentPage + 1
                
                Log.d("OrderViewModel", "Loading page $nextPage for buyer: ${buyer.take(8)}...")
                
                getOrdersByBuyerPagedUseCase(buyer, nextPage, PAGE_SIZE)
                    .catch { e ->
                        Log.e("OrderViewModel", "Error loading more orders: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            hasMoreOrders = false,
                            isLoadingMore = false
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { newOrders ->
                                Log.d("OrderViewModel", "Fetched ${newOrders.size} more orders for page $nextPage")
                                
                                if (newOrders.isEmpty()) {
                                    _uiState.value = _uiState.value.copy(
                                        hasMoreOrders = false,
                                        isLoadingMore = false
                                    )
                                } else {
                                    val currentOrders = _uiState.value.orders
                                    val updatedOrders = currentOrders + newOrders
                                    
                                    Log.d("OrderViewModel", "Total orders after update: ${updatedOrders.size} (was ${currentOrders.size}, added ${newOrders.size})")
                                    
                                    _uiState.value = _uiState.value.copy(
                                        orders = updatedOrders,
                                        currentPage = nextPage,
                                        hasMoreOrders = newOrders.size == PAGE_SIZE,
                                        isLoadingMore = false,
                                        totalOrders = updatedOrders.size
                                    )
                                }
                            },
                            onFailure = { e ->
                                Log.e("OrderViewModel", "Failed to load more orders: ${e.message}")
                                _uiState.value = _uiState.value.copy(
                                    hasMoreOrders = false,
                                    isLoadingMore = false
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error loading more orders: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load more orders: ${e.message}",
                    isLoadingMore = false
                )
            }
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val order = getOrderByIdUseCase(orderId)
                if (order != null) {
                    _uiState.value = _uiState.value.copy(
                        orders = listOf(order),
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "订单未找到"
                    )
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
        selectedAddress: com.focx.domain.entity.UserAddress?,
        orderNote: String,
        activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
        onResult: (Result<Order>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val buyer = getCurrentWalletAddressUseCase.execute()!!
                
                // Validate that an address is selected
                if (selectedAddress == null) {
                    throw IllegalArgumentException("Please select a delivery address")
                }
                
                val result = createOrderUseCase(
                    product = product, 
                    quantity = quantity, 
                    buyer = buyer, 
                    shippingAddress = selectedAddress.toShippingAddress(),
                    orderNote = orderNote,
                    activityResultSender = activityResultSender
                )
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
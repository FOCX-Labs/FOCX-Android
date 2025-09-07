package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetOrdersBySellerPagedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject
import com.focx.utils.Log

data class SellerOrderListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val orders: List<Order> = emptyList(),
    val currentPage: Int = 0,
    val hasMoreOrders: Boolean = true,
    val totalOrders: Int = 0
)

@HiltViewModel
class SellerOrderListViewModel @Inject constructor(
    private val getOrdersBySellerPagedUseCase: GetOrdersBySellerPagedUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrderListUiState())
    val uiState: StateFlow<SellerOrderListUiState> = _uiState.asStateFlow()

    private val PAGE_SIZE = 5 // Number of orders to load per page
    
    fun resetAndLoadOrders() {
        Log.d("SellerOrderListViewModel", "resetAndLoadOrders called")
        
        // Cancel any ongoing coroutines to prevent interference
        viewModelScope.coroutineContext.cancelChildren()
        
        // Force reset state completely
        _uiState.value = SellerOrderListUiState()
        
        // Add a small delay to ensure state is fully reset
        viewModelScope.launch {
            kotlinx.coroutines.delay(50) // Small delay to ensure state reset
            loadSellerOrders()
        }
    }

    fun loadSellerOrders() {
        Log.d("SellerOrderListViewModel", "loadSellerOrders called")
        
        // Reset state and start fresh load
        _uiState.value = SellerOrderListUiState(isLoading = true)
        
        viewModelScope.launch {
            try {
                val merchant = getCurrentWalletAddressUseCase.execute()!!
                getOrdersBySellerPagedUseCase(merchant, 1, PAGE_SIZE)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load orders: ${e.message}"
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { orders ->
                                Log.d("SellerOrderListViewModel", "Initial orders fetched: ${orders.size}")
                                
                                _uiState.value = _uiState.value.copy(
                                    orders = orders,
                                    currentPage = 1,
                                    hasMoreOrders = orders.size == PAGE_SIZE,
                                    totalOrders = orders.size,
                                    isLoading = false
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
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun loadMoreOrders() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreOrders) {
            Log.d("SellerOrderListViewModel", "loadMoreOrders skipped - isLoadingMore: ${_uiState.value.isLoadingMore}, hasMoreOrders: ${_uiState.value.hasMoreOrders}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val merchant = getCurrentWalletAddressUseCase.execute()!!
                val nextPage = _uiState.value.currentPage + 1
                
                Log.d("SellerOrderListViewModel", "Loading page $nextPage")
                
                getOrdersBySellerPagedUseCase(merchant, nextPage, PAGE_SIZE)
                    .catch { e ->
                        Log.e("SellerOrderListViewModel", "Error loading more orders: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            hasMoreOrders = false,
                            isLoadingMore = false
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { newOrders ->
                                Log.d("SellerOrderListViewModel", "Fetched ${newOrders.size} more orders")
                                
                                if (newOrders.isEmpty()) {
                                    _uiState.value = _uiState.value.copy(
                                        hasMoreOrders = false,
                                        isLoadingMore = false
                                    )
                                } else {
                                    val currentState = _uiState.value
                                    val updatedOrders = currentState.orders + newOrders
                                    
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
                                Log.e("SellerOrderListViewModel", "Failed to load more orders: ${e.message}")
                                _uiState.value = _uiState.value.copy(
                                    hasMoreOrders = false,
                                    isLoadingMore = false
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("SellerOrderListViewModel", "Error loading more orders: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load more orders: ${e.message}",
                    isLoadingMore = false
                )
            }
        }
    }

    fun refreshData() {
        Log.d("SellerOrderListViewModel", "refreshData called")
        loadSellerOrders()
    }
}
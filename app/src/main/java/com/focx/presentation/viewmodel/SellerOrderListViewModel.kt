package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetOrdersBySellerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.focx.utils.Log

data class SellerOrderListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val orders: List<Order> = emptyList(),
    val isDataCached: Boolean = false,
    val lastRefreshTime: Long = 0L
)

@HiltViewModel
class SellerOrderListViewModel @Inject constructor(
    private val getOrdersBySellerUseCase: GetOrdersBySellerUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrderListUiState())
    val uiState: StateFlow<SellerOrderListUiState> = _uiState.asStateFlow()

    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes cache duration

    fun loadSellerOrders(forceRefresh: Boolean = false) {
        Log.d("SellerOrderListViewModel", "loadSellerOrders called with forceRefresh: $forceRefresh")
        val currentTime = System.currentTimeMillis()
        val currentState = _uiState.value
        
        // Check if we have cached data and it's still valid
        if (!forceRefresh && 
            currentState.isDataCached && 
            currentState.orders.isNotEmpty() &&
            (currentTime - currentState.lastRefreshTime) < CACHE_DURATION) {
            Log.d("SellerOrderListViewModel", "Using cached data, skipping refresh")
            return // Use cached data
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val merchant = getCurrentWalletAddressUseCase.execute()!!
                getOrdersBySellerUseCase(merchant)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load orders: ${e.message}"
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { orders ->
                                _uiState.value = _uiState.value.copy(
                                    orders = orders,
                                    isDataCached = true,
                                    lastRefreshTime = System.currentTimeMillis()
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to load orders: ${e.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load data: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshData() {
        Log.d("SellerOrderListViewModel", "refreshData called")
        loadSellerOrders(forceRefresh = true)
    }

    fun clearCache() {
        _uiState.value = _uiState.value.copy(
            isDataCached = false,
            lastRefreshTime = 0L
        )
    }
}
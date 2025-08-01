package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.entity.Order
import com.focx.domain.entity.Product
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetMerchantStatusUseCase
import com.focx.domain.usecase.GetOrdersBySellerUseCase
import com.focx.domain.usecase.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SellUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sellerStats: MerchantStatus? = null,
    val myProducts: List<Product> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val isDataCached: Boolean = false,
    val lastRefreshTime: Long = 0L
)

@HiltViewModel
class SellViewModel @Inject constructor(
    private val getSellerStatsUseCase: GetMerchantStatusUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getOrdersBySellerUseCase: GetOrdersBySellerUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellUiState())
    val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes cache duration

    fun loadSellerData(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        val currentState = _uiState.value
        
        // Check if we have cached data and it's still valid
        if (!forceRefresh && 
            currentState.isDataCached && 
            currentState.sellerStats != null && 
            currentState.myProducts.isNotEmpty() &&
            (currentTime - currentState.lastRefreshTime) < CACHE_DURATION) {
            return // Use cached data
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load data in parallel
                launch {
                    val merchant = getCurrentWalletAddressUseCase.execute()!!
                    getSellerStatsUseCase(merchant)
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load seller statistics: ${e.message}"
                            )
                        }
                        .collect { stats ->
                            _uiState.value = _uiState.value.copy(sellerStats = stats)
                        }
                }

                launch {
                    getProductsUseCase(1, 20) // TODO: Implement proper pagination
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load products: ${e.message}"
                            )
                        }
                        .collect { result ->
                            result.fold(
                                onSuccess = { products ->
                                    // Only show the current seller's products (top 5)
                                    val myProducts = products.filter { it.sellerId == "current_seller_id" }.take(5)
                                    _uiState.value = _uiState.value.copy(myProducts = myProducts)
                                },
                                onFailure = { e ->
                                    _uiState.value = _uiState.value.copy(
                                        error = "Failed to load products: ${e.message}"
                                    )
                                }
                            )
                        }
                }

                launch {
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
                                    // Only show the 5 most recent orders
                                    val recentOrders = orders.take(5)
                                    _uiState.value = _uiState.value.copy(recentOrders = recentOrders)
                                },
                                onFailure = { e ->
                                    _uiState.value = _uiState.value.copy(
                                        error = "Failed to load orders: ${e.message}"
                                    )
                                }
                            )
                        }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load data: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDataCached = true,
                    lastRefreshTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun refreshData() {
        loadSellerData(forceRefresh = true)
    }

    fun clearCache() {
        _uiState.value = _uiState.value.copy(
            isDataCached = false,
            lastRefreshTime = 0L
        )
    }
}
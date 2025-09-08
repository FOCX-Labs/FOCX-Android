package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Product
import com.focx.domain.usecase.GetMerchantProductsUseCase
import com.focx.domain.usecase.GetMerchantProductsPagedUseCase
import com.focx.utils.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject

data class SellerManagementUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val merchantAddress: String? = null,
    val currentPage: Int = 0,
    val hasMoreProducts: Boolean = true,
    val totalProducts: Int = 0
)

@HiltViewModel
class SellerManagementViewModel @Inject constructor(
    private val getMerchantProductsUseCase: GetMerchantProductsUseCase,
    private val getMerchantProductsPagedUseCase: GetMerchantProductsPagedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerManagementUiState())
    val uiState: StateFlow<SellerManagementUiState> = _uiState.asStateFlow()
    
    private val PAGE_SIZE = 5 // Number of products to load per page

    fun resetAndLoadProducts(merchantAddress: String) {
        Log.d("SellerManagementViewModel", "resetAndLoadProducts called")
        
        // Cancel any ongoing coroutines to prevent interference
        viewModelScope.coroutineContext.cancelChildren()
        
        // Force reset state completely
        _uiState.value = SellerManagementUiState()
        
        // Add a small delay to ensure state is fully reset
        viewModelScope.launch {
            kotlinx.coroutines.delay(50) // Small delay to ensure state reset
            loadMerchantProducts(merchantAddress)
        }
    }

    fun loadMerchantProducts(merchantAddress: String) {
        Log.d("SellerManagementViewModel", "loadMerchantProducts called")
        
        // Reset state and start fresh load
        _uiState.value = SellerManagementUiState(isLoading = true, merchantAddress = merchantAddress)
        
        viewModelScope.launch {
            try {
                getMerchantProductsPagedUseCase(merchantAddress, 1, PAGE_SIZE)
                    .collect { result ->
                        result.fold(
                            onSuccess = { products ->
                                Log.d("SellerManagementViewModel", "Initial products fetched: ${products.size}")
                                Log.d("SellerManagementViewModel", "Initial product IDs: ${products.map { it.id }}")
                                _uiState.value = _uiState.value.copy(
                                    products = products,
                                    currentPage = 1,
                                    hasMoreProducts = products.isNotEmpty(),
                                    totalProducts = products.size,
                                    isLoading = false,
                                    error = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("SellerManagementViewModel", "Failed to load merchant products: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load products: ${error.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("SellerManagementViewModel", "Exception in loadMerchantProducts: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load products: ${e.message}"
                )
            }
        }
    }
    
    fun loadMoreProducts() {
        val currentState = _uiState.value
        val merchantAddress = currentState.merchantAddress
        
        Log.d("SellerManagementViewModel", "loadMoreProducts called - currentPage: ${currentState.currentPage}, hasMore: ${currentState.hasMoreProducts}, isLoadingMore: ${currentState.isLoadingMore}, productsCount: ${currentState.products.size}")
        
        if (currentState.isLoadingMore || !currentState.hasMoreProducts || merchantAddress == null) {
            Log.d("SellerManagementViewModel", "loadMoreProducts skipped - isLoadingMore: ${currentState.isLoadingMore}, hasMoreProducts: ${currentState.hasMoreProducts}, merchantAddress: $merchantAddress")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val nextPage = currentState.currentPage + 1
                
                Log.d("SellerManagementViewModel", "Loading page $nextPage for merchant: ${merchantAddress.take(8)}...")
                
                getMerchantProductsPagedUseCase(merchantAddress, nextPage, PAGE_SIZE)
                    .catch { e ->
                        Log.e("SellerManagementViewModel", "Error loading more products: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            hasMoreProducts = false,
                            isLoadingMore = false
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { newProducts ->
                                Log.d("SellerManagementViewModel", "Fetched ${newProducts.size} more products for page $nextPage")
                                Log.d("SellerManagementViewModel", "New product IDs: ${newProducts.map { it.id }}")
                                
                                // ShopUtils handles the counting logic internally
                                // Empty result means we've reached the end of the counter range
                                if (newProducts.isEmpty()) {
                                    Log.d("SellerManagementViewModel", "No more products available - counter range exhausted")
                                    _uiState.value = _uiState.value.copy(
                                        hasMoreProducts = false,
                                        isLoadingMore = false
                                    )
                                } else {
                                    val currentProducts = _uiState.value.products
                                    Log.d("SellerManagementViewModel", "Current product IDs: ${currentProducts.map { it.id }}")
                                    
                                    // Check for duplicates before adding
                                    val currentIds = currentProducts.map { it.id }.toSet()
                                    val duplicateIds = newProducts.map { it.id }.filter { it in currentIds }
                                    if (duplicateIds.isNotEmpty()) {
                                        Log.e("SellerManagementViewModel", "Found duplicate product IDs: $duplicateIds")
                                        // Filter out duplicates as protection
                                        val uniqueNewProducts = newProducts.filter { it.id !in currentIds }
                                        
                                        if (uniqueNewProducts.isEmpty()) {
                                            // All products were duplicates - we've reached the end
                                            Log.d("SellerManagementViewModel", "All products were duplicates - stopping pagination")
                                            _uiState.value = _uiState.value.copy(
                                                hasMoreProducts = false,
                                                isLoadingMore = false
                                            )
                                        } else {
                                            val updatedProducts = currentProducts + uniqueNewProducts
                                            Log.d("SellerManagementViewModel", "Added ${uniqueNewProducts.size} unique products after filtering duplicates")
                                            
                                            _uiState.value = _uiState.value.copy(
                                                products = updatedProducts,
                                                currentPage = nextPage,
                                                hasMoreProducts = true,
                                                isLoadingMore = false,
                                                totalProducts = updatedProducts.size
                                            )
                                        }
                                    } else {
                                        val updatedProducts = currentProducts + newProducts
                                        
                                        Log.d("SellerManagementViewModel", "Total products after update: ${updatedProducts.size} (was ${currentProducts.size}, added ${newProducts.size})")
                                        Log.d("SellerManagementViewModel", "All product IDs after update: ${updatedProducts.map { it.id }}")
                                        
                                        _uiState.value = _uiState.value.copy(
                                            products = updatedProducts,
                                            currentPage = nextPage,
                                            hasMoreProducts = true,
                                            isLoadingMore = false,
                                            totalProducts = updatedProducts.size
                                        )
                                    }
                                }
                            },
                            onFailure = { e ->
                                Log.e("SellerManagementViewModel", "Failed to load more products: ${e.message}")
                                _uiState.value = _uiState.value.copy(
                                    hasMoreProducts = false,
                                    isLoadingMore = false
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("SellerManagementViewModel", "Error loading more products: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load more products: ${e.message}",
                    isLoadingMore = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh(merchantAddress: String) {
        Log.d("SellerManagementViewModel", "Refreshing merchant products for address: $merchantAddress")
        resetAndLoadProducts(merchantAddress)
    }
} 
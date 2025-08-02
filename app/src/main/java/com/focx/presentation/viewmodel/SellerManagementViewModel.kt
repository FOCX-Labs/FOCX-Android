package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Product
import com.focx.domain.usecase.GetMerchantProductsUseCase
import com.focx.utils.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SellerManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val merchantAddress: String? = null
)

@HiltViewModel
class SellerManagementViewModel @Inject constructor(
    private val getMerchantProductsUseCase: GetMerchantProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerManagementUiState())
    val uiState: StateFlow<SellerManagementUiState> = _uiState.asStateFlow()

    fun loadMerchantProducts(merchantAddress: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                merchantAddress = merchantAddress
            )

            try {
                getMerchantProductsUseCase(merchantAddress)
                    .collect { result ->
                        result.fold(
                            onSuccess = { products ->
                                Log.d("SellerManagementViewModel", "Loaded ${products.size} products")
                                _uiState.value = _uiState.value.copy(
                                    products = products,
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh(merchantAddress: String) {
        loadMerchantProducts(merchantAddress)
    }
} 
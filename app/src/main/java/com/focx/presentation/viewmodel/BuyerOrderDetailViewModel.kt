package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.usecase.GetOrderByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyerOrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BuyerOrderDetailViewModel @Inject constructor(
    private val getOrderByIdUseCase: GetOrderByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BuyerOrderDetailState())
    val state: StateFlow<BuyerOrderDetailState> = _state.asStateFlow()

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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
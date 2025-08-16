package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Order
import com.focx.domain.entity.ProposalType
import com.focx.domain.usecase.GetOrderByIdUseCase
import com.focx.domain.usecase.ConfirmReceiptUseCase
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.InitiateDisputeUseCase
import com.focx.domain.usecase.CreateProposalUseCase
import com.solana.publickey.SolanaPublicKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyerOrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDisputeDialog: Boolean = false
)

@HiltViewModel
class BuyerOrderDetailViewModel @Inject constructor(
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val confirmReceiptUseCase: ConfirmReceiptUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase,
    private val initiateDisputeUseCase: InitiateDisputeUseCase,
    private val createProposalUseCase: CreateProposalUseCase
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

    fun confirmReceipt(orderId: String, sellerId: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val walletAddress = getCurrentWalletAddressUseCase.execute()!!
                val result = confirmReceiptUseCase(
                    orderId,
                    SolanaPublicKey.from(walletAddress),
                    SolanaPublicKey.from(sellerId),
                    activityResultSender
                )
                
                if (result.isSuccess) {
                    // Reload order after successful confirmation
                    loadOrder(orderId)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to confirm receipt"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to confirm receipt"
                )
            }
        }
    }

    fun initiateDispute(orderId: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val walletAddress = getCurrentWalletAddressUseCase.execute()!!
                val result = initiateDisputeUseCase(orderId, SolanaPublicKey.from(walletAddress), activityResultSender)
                
                if (result.isSuccess) {
                    // Reload order after successful dispute initiation
                    loadOrder(orderId)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to initiate dispute"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to initiate dispute"
                )
            }
        }
    }

    fun showDisputeDialog() {
        _state.value = _state.value.copy(showDisputeDialog = true)
    }

    fun hideDisputeDialog() {
        _state.value = _state.value.copy(showDisputeDialog = false)
    }

    fun createDisputeProposal(
        title: String,
        description: String,
        orderId: String,
        activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val walletAddress = getCurrentWalletAddressUseCase.execute()!!
                val result = createProposalUseCase.execute(
                    title,
                    description,
                    ProposalType.DISPUTE,
                    SolanaPublicKey.from(walletAddress),
                    activityResultSender
                )
                
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showDisputeDialog = false
                    )
                    // Reload order after successful proposal creation
                    loadOrder(orderId)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to create dispute proposal"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create dispute proposal"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

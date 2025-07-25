package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.core.constants.AppConstants
import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetMerchantStatusUseCase
import com.focx.domain.usecase.RegisterMerchantUseCase
import com.focx.utils.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.http.cio.internals.parseDecLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SellerRegistrationUiState(
    val isRegistered: Boolean = false,
    val isLoading: Boolean = false,
    val storeName: String = "",
    val storeDescription: String = "",
    val errorMessage: String? = null,
    val isRegistrationInProgress: Boolean = false,
    val registrationSuccess: Boolean = false,
    val transactionSignature: String? = null,
    val isWalletConnected: Boolean = false,
    val walletAddress: String? = null,
    val merchantAccount: String? = null,
    val estimatedCost: RegistrationCost? = null,
    val accountsCreated: List<CreatedAccount> = emptyList(),
    val securityDeposit: String = AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT.toString()
)

data class RegistrationCost(
    val transactionFee: Double = 0.000005, // ~0.000005 SOL
    val merchantInfoRent: Double = 0.005638, // Merchant account rent
    val merchantIdAccountRent: Double = 0.02353176, // MerchantIdAccount rent
    val idChunkRent: Double = 0.0098484, // IdChunk rent
    val totalCost: Double = transactionFee + merchantInfoRent + merchantIdAccountRent + idChunkRent
)

data class CreatedAccount(
    val accountType: String,
    val accountAddress: String,
    val rentCost: Double,
    val transactionSignature: String
)

@HiltViewModel
class SellerRegistrationViewModel @Inject constructor(
    private val registerMerchantUseCase: RegisterMerchantUseCase,
    private val getMerchantStatusUseCase: GetMerchantStatusUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerRegistrationUiState())
    val uiState: StateFlow<SellerRegistrationUiState> = _uiState.asStateFlow()

    init {
        checkMerchantStatus()
        calculateEstimatedCost()
    }

    private fun calculateEstimatedCost() {
        val estimatedCost = RegistrationCost()
        _uiState.value = _uiState.value.copy(estimatedCost = estimatedCost)
    }

    private fun checkMerchantStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val walletAddress = getCurrentWalletAddressUseCase.execute()
                val isWalletConnected = getCurrentWalletAddressUseCase.isWalletConnected()

                if (walletAddress != null && isWalletConnected) {
                    val merchantStatus = getMerchantStatusUseCase(walletAddress).first()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = merchantStatus.isRegistered,
                        isWalletConnected = true,
                        walletAddress = walletAddress
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isWalletConnected = false,
                        walletAddress = null,
                        errorMessage = "Please connect your wallet first to check merchant status"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to check merchant status: ${e.message}"
                )
            }
        }
    }

    fun updateStoreName(name: String) {
        _uiState.value = _uiState.value.copy(storeName = name)
    }

    fun updateDeposit(name: String) {
        val digitsOnly = name.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(securityDeposit = digitsOnly)
    }

    fun updateStoreDescription(description: String) {
        _uiState.value = _uiState.value.copy(storeDescription = description)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun registerAsSeller(activityResultSender: ActivityResultSender) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Check wallet connection first
            val walletAddress = getCurrentWalletAddressUseCase.execute()
            val isWalletConnected = getCurrentWalletAddressUseCase.isWalletConnected()

            val securityDeposit = currentState.securityDeposit.parseDecLong()

            if (securityDeposit < AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT) {
                _uiState.value = currentState.copy(
                    errorMessage = "The minimum deposit is 1,000 USDC"
                )
                return@launch
            }

            if (!isWalletConnected || walletAddress == null) {
                _uiState.value = currentState.copy(
                    errorMessage = "Please connect your wallet first to register as a seller"
                )
                return@launch
            }

            // Validate input
            if (currentState.storeName.isBlank()) {
                _uiState.value = currentState.copy(errorMessage = "Store name is required")
                return@launch
            }

            if (currentState.storeDescription.isBlank()) {
                _uiState.value = currentState.copy(errorMessage = "Store description is required")
                return@launch
            }

            _uiState.value = currentState.copy(
                isRegistrationInProgress = true,
                errorMessage = null
            )

            try {
                // Use actual wallet address for both merchant and payer
                val merchantPublicKey = walletAddress
                val payerPublicKey = walletAddress

                val merchantRegistration = MerchantRegistration(
                    name = currentState.storeName,
                    description = currentState.storeDescription,
                    merchantPublicKey = merchantPublicKey,
                    payerPublicKey = payerPublicKey,
                    securityDeposit = securityDeposit, // 1,000 USDC as mentioned in UI
                    programId = AppConstants.App.PROGRAM_ID
                )

                val result = registerMerchantUseCase(merchantRegistration, activityResultSender)

                if (result.success) {
                    Log.d("SellerRegistration", "Registration successful, creating accounts list...")

                    // Create accounts created list based on the registration result
                    val accountsCreated = mutableListOf<CreatedAccount>()
                    val estimatedCost = currentState.estimatedCost ?: RegistrationCost()

                    result.transactionSignature?.let { signature ->
                        Log.d("SellerRegistration", "Processing transaction signature: $signature")

                        result.merchantAccount?.let { merchantAccount ->
                            val merchantInfoAccount = CreatedAccount(
                                accountType = "Merchant Info Account",
                                accountAddress = merchantAccount,
                                rentCost = estimatedCost.merchantInfoRent,
                                transactionSignature = signature
                            )
                            accountsCreated.add(merchantInfoAccount)
                            Log.d(
                                "SellerRegistration",
                                "Added merchant info account: ${merchantInfoAccount.accountAddress}"
                            )
                        }

                        // Add other accounts that would be created
                        val merchantIdAccount = CreatedAccount(
                            accountType = "Merchant ID Account",
                            accountAddress = "${merchantPublicKey}_merchant_id",
                            rentCost = estimatedCost.merchantIdAccountRent,
                            transactionSignature = signature
                        )
                        accountsCreated.add(merchantIdAccount)
                        Log.d("SellerRegistration", "Added merchant ID account: ${merchantIdAccount.accountAddress}")

                        val idChunkAccount = CreatedAccount(
                            accountType = "ID Chunk Account",
                            accountAddress = "${merchantPublicKey}_id_chunk_0",
                            rentCost = estimatedCost.idChunkRent,
                            transactionSignature = signature
                        )
                        accountsCreated.add(idChunkAccount)
                        Log.d("SellerRegistration", "Added ID chunk account: ${idChunkAccount.accountAddress}")
                    }

                    Log.i("SellerRegistration", "Total accounts created: ${accountsCreated.size}")
                    Log.d(
                        "SellerRegistration",
                        "Accounts created details: ${accountsCreated.map { "${it.accountType}: ${it.accountAddress}" }}"
                    )

                    _uiState.value = currentState.copy(
                        isRegistrationInProgress = false,
                        registrationSuccess = true,
                        isRegistered = true,
                        transactionSignature = result.transactionSignature,
                        merchantAccount = result.merchantAccount,
                        accountsCreated = accountsCreated
                    )

                    Log.i(
                        "SellerRegistration",
                        "Registration completed successfully with ${accountsCreated.size} accounts created"
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isRegistrationInProgress = false,
                        errorMessage = result.errorMessage ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isRegistrationInProgress = false,
                    errorMessage = "Registration failed: ${e.message}"
                )
            }
        }
    }

    fun setRegistered(isRegistered: Boolean) {
        _uiState.value = _uiState.value.copy(isRegistered = isRegistered)
    }

    fun refreshWalletStatus() {
        checkMerchantStatus()
    }
}
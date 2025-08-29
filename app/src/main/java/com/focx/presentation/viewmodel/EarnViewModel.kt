package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.StakeActivity
import com.focx.domain.entity.VaultDepositor
import com.focx.domain.entity.Vault
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetStakeActivitiesUseCase
import com.focx.domain.usecase.GetStakingInfoUseCase
import com.focx.domain.usecase.GetVaultInfoUseCase
import com.focx.domain.usecase.GetVaultInfoWithStakersUseCase
import com.focx.domain.usecase.InitializeVaultDepositorUseCase
import com.focx.domain.usecase.SolanaTokenBalanceUseCase
import com.focx.domain.usecase.StakeUsdcUseCase
import com.focx.domain.usecase.RequestUnstakeUsdcUseCase
import com.focx.domain.usecase.UnstakeUsdcUseCase
import com.focx.utils.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarnUiState(
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val error: String? = null,
    val vault: Vault? = null,
    val totalStakers: Int = 0,
    val stakingInfo: VaultDepositor? = null,
    val stakeActivities: List<StakeActivity> = emptyList(),
    val userWalletAddress: String? = null,
    val showInitializeVaultDepositorDialog: Boolean = false,
    val pendingStakeAmount: ULong? = null,
    val usdcBalance: Long = 0L
)

@HiltViewModel
class EarnViewModel @Inject constructor(
    private val getVaultInfoUseCase: GetVaultInfoUseCase,
    private val getVaultInfoWithStakersUseCase: GetVaultInfoWithStakersUseCase,
    private val getStakingInfoUseCase: GetStakingInfoUseCase,
    private val getStakeActivitiesUseCase: GetStakeActivitiesUseCase,
    private val stakeUsdcUseCase: StakeUsdcUseCase,
    private val requestUnstakeUsdcUseCase: RequestUnstakeUsdcUseCase,
    private val unstakeUsdcUseCase: UnstakeUsdcUseCase,
    private val initializeVaultDepositorUseCase: InitializeVaultDepositorUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase,
    private val solanaTokenBalanceUseCase: SolanaTokenBalanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarnUiState())
    val uiState: StateFlow<EarnUiState> = _uiState.asStateFlow()

    fun loadEarnData() {
        viewModelScope.launch {
            val walletAddress = getCurrentWalletAddressUseCase.execute()
            if (walletAddress == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialLoading = false,
                    error = "Wallet not connected"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isInitialLoading = true,
                error = null,
                userWalletAddress = walletAddress
            )

            try {
                // Load vault info with stakers
                getVaultInfoWithStakersUseCase(walletAddress).collect { result ->
                    result.fold(
                        onSuccess = { vaultInfoWithStakers ->
                            _uiState.value = _uiState.value.copy(
                                vault = vaultInfoWithStakers.vault,
                                totalStakers = vaultInfoWithStakers.totalStakers
                            )
                        },
                        onFailure = { error ->
                            Log.e("EarnViewModel", "Failed to load vault info: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load vault info: ${error.message}"
                            )
                        }
                    )
                }

                // Load staking info
                getStakingInfoUseCase(walletAddress).collect { result ->
                    result.fold(
                        onSuccess = { stakingInfo ->
                            _uiState.value = _uiState.value.copy(stakingInfo = stakingInfo)
                        },
                        onFailure = { error ->
                            Log.e("EarnViewModel", "Failed to load staking info: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load staking info: ${error.message}"
                            )
                        }
                    )
                }

                // Load stake activities
                getStakeActivitiesUseCase(walletAddress).collect { result ->
                    result.fold(
                        onSuccess = { activities ->
                            _uiState.value = _uiState.value.copy(stakeActivities = activities)
                        },
                        onFailure = { error ->
                            Log.e("EarnViewModel", "Failed to load stake activities: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load stake activities: ${error.message}"
                            )
                        }
                    )
                }

                // Load USDC balance
                try {
                    val userPublicKey = SolanaPublicKey.from(walletAddress)
                    val balance = solanaTokenBalanceUseCase.getBalanceByOwnerAndMint(userPublicKey)
                    _uiState.value = _uiState.value.copy(usdcBalance = balance)
                    Log.d("EarnViewModel", "USDC balance loaded: $balance")
                } catch (e: Exception) {
                    Log.e("EarnViewModel", "Failed to load USDC balance: ${e.message}", e)
                    // Don't set error for balance failure, just log it
                }
            } catch (e: Exception) {
                Log.e("EarnViewModel", "Exception in loadEarnData: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load earn data: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false, isInitialLoading = false)
            }
        }
    }

    fun stakeUsdc(amount: ULong, activityResultSender: ActivityResultSender) {
        val walletAddress = _uiState.value.userWalletAddress
        if (walletAddress == null) {
            _uiState.value = _uiState.value.copy(
                error = "Wallet not connected"
            )
            return
        }

        // Check if VaultDepositor is null, if so show confirmation dialog
        if (_uiState.value.stakingInfo == null) {
            _uiState.value = _uiState.value.copy(
                showInitializeVaultDepositorDialog = true,
                pendingStakeAmount = amount
            )
            return
        }

        performStakeUsdc(amount, activityResultSender)
    }

    private fun performStakeUsdc(amount: ULong, activityResultSender: ActivityResultSender) {
        val walletAddress = _uiState.value.userWalletAddress
        if (walletAddress == null) {
            _uiState.value = _uiState.value.copy(
                error = "Wallet not connected"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                stakeUsdcUseCase(walletAddress, amount, activityResultSender)
                    .collect { result ->
                        result.fold(
                            onSuccess = { signature ->
                                Log.d("EarnViewModel", "Stake USDC successful: $signature")
                                // Refresh data after successful stake
                                loadEarnData()
                                _uiState.value = _uiState.value.copy(
                                    error = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("EarnViewModel", "Stake USDC failed: ${error.message}")
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("EarnViewModel", "Exception in stakeUsdc: ${e.message}", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun requestUnstakeUsdc(amount: ULong, activityResultSender: ActivityResultSender) {
        val walletAddress = _uiState.value.userWalletAddress
        if (walletAddress == null) {
            _uiState.value = _uiState.value.copy(
                error = "Wallet not connected"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                requestUnstakeUsdcUseCase(walletAddress, amount, activityResultSender)
                    .collect { result ->
                        result.fold(
                            onSuccess = { signature ->
                                Log.d("EarnViewModel", "Unstake USDC successful: $signature")
                                // Refresh data after successful unstake
                                loadEarnData()
                                _uiState.value = _uiState.value.copy(
                                    error = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("EarnViewModel", "Unstake USDC failed: ${error.message}")
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("EarnViewModel", "Exception in unstakeUsdc: ${e.message}", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun unstakeUsdc(amount: ULong, activityResultSender: ActivityResultSender) {
        val walletAddress = _uiState.value.userWalletAddress
        if (walletAddress == null) {
            _uiState.value = _uiState.value.copy(
                error = "Wallet not connected"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                unstakeUsdcUseCase(walletAddress, amount, activityResultSender)
                    .collect { result ->
                        result.fold(
                            onSuccess = { signature ->
                                Log.d("EarnViewModel", "Unstake USDC successful: $signature")
                                // Refresh data after successful unstake
                                loadEarnData()
                                _uiState.value = _uiState.value.copy(
                                    error = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("EarnViewModel", "Unstake USDC failed: ${error.message}")
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("EarnViewModel", "Exception in unstakeUsdc: ${e.message}", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadEarnData()
    }

    fun dismissInitializeVaultDepositorDialog() {
        _uiState.value = _uiState.value.copy(
            showInitializeVaultDepositorDialog = false,
            pendingStakeAmount = null
        )
    }

    fun confirmInitializeVaultDepositor(activityResultSender: ActivityResultSender) {
        val walletAddress = _uiState.value.userWalletAddress
        val pendingAmount = _uiState.value.pendingStakeAmount
        
        if (walletAddress == null || pendingAmount == null) {
            _uiState.value = _uiState.value.copy(
                error = "Invalid state for initialization"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                initializeVaultDepositorUseCase(walletAddress, activityResultSender)
                    .collect { result ->
                        result.fold(
                            onSuccess = { signature ->
                                Log.d("EarnViewModel", "Initialize vault depositor successful: $signature")
                                // After successful initialization, proceed with stake
                                performStakeUsdc(pendingAmount, activityResultSender)
                                _uiState.value = _uiState.value.copy(
                                    showInitializeVaultDepositorDialog = false,
                                    pendingStakeAmount = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("EarnViewModel", "Initialize vault depositor failed: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    showInitializeVaultDepositorDialog = false,
                                    pendingStakeAmount = null
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("EarnViewModel", "Exception in confirmInitializeVaultDepositor: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    showInitializeVaultDepositorDialog = false,
                    pendingStakeAmount = null
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
} 
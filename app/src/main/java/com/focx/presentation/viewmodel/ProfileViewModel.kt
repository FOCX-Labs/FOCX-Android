package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Connected
import com.focx.domain.entity.StakingInfo
import com.focx.domain.entity.User
import com.focx.domain.entity.UserAddress
import com.focx.domain.entity.WalletBalance
import com.focx.domain.usecase.ConnectWalletUseCase
import com.focx.domain.usecase.DisconnectWalletUseCase
import com.focx.domain.usecase.GetCurrentUserUseCase
import com.focx.domain.usecase.GetStakingInfoUseCase
import com.focx.domain.usecase.GetUserAddressesUseCase
import com.focx.domain.usecase.GetWalletBalanceUseCase
import com.focx.domain.usecase.LoginWithWalletUseCase
import com.focx.domain.usecase.RequestUsdcFaucetUseCase
import com.focx.domain.usecase.SolanaAccountBalanceUseCase
import com.focx.domain.usecase.SolanaTokenBalanceUseCase
import com.focx.domain.usecase.SolanaWalletConnectUseCase
import com.focx.domain.usecase.WalletConnectResult
import com.focx.utils.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.focx.data.datasource.local.AddressLocalDataSource
import com.focx.utils.ShopUtils

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val walletBalance: WalletBalance? = null,
    val stakingInfo: StakingInfo? = null,
    val userAddresses: List<UserAddress> = emptyList(),
    val isWalletConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserAddressesUseCase: GetUserAddressesUseCase,
    private val getWalletBalanceUseCase: GetWalletBalanceUseCase,
    private val solanaAccountBalanceUseCase: SolanaAccountBalanceUseCase,
    private val solanaTokenBalanceUseCase: SolanaTokenBalanceUseCase,
    private val getStakingInfoUseCase: GetStakingInfoUseCase,
    private val connectWalletUseCase: ConnectWalletUseCase,
    private val disconnectWalletUseCase: DisconnectWalletUseCase,
    private val loginWithWalletUseCase: LoginWithWalletUseCase,
    private val solanaWalletConnectUseCase: SolanaWalletConnectUseCase,
    private val requestUsdcFaucetUseCase: RequestUsdcFaucetUseCase,
    private val addressLocalDataSource: AddressLocalDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private fun loadProfileDataWithStoredConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Check for stored wallet connection
            val storedConnection = solanaWalletConnectUseCase.getStoredConnection()

            if (storedConnection is Connected) {
                // Auto-login with stored wallet connection
                loginWithWalletUseCase(storedConnection.publicKey.base58()).fold(
                    onSuccess = { user ->
                        Log.d("ProfileViewModel", "Auto-login successful for user: ${user.id}")
                        _uiState.value = _uiState.value.copy(
                            user = user,
                            isWalletConnected = true
                        )
                        // Load wallet data and user addresses
                        user.walletAddress?.let { address ->
                            loadWalletData(address)
                        }
                        loadUserAddresses()
                        if (user.walletAddress == null) {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    },
                    onFailure = { error ->
                        Log.e("ProfileViewModel", "Auto-login failed: ${error.message}")
                        // Clear invalid stored connection
                        solanaWalletConnectUseCase.disconnect()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = null,
                            isWalletConnected = false
                        )
                    }
                )
            } else {
                // No stored connection - user needs to connect wallet
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = null,
                    isWalletConnected = false
                )
            }
        }
    }


    fun loadProfileData() {
        loadProfileDataWithStoredConnection()
    }
    
    /**
     * Load user addresses only, without requiring wallet connection
     */
    fun loadUserAddressesOnly() {
        loadUserAddresses()
    }

    private fun loadWalletData(walletAddress: String) {
        viewModelScope.launch {
            try {
                // Check if wallet address is valid
                if (walletAddress.isBlank()) {
                    Log.w("ProfileViewModel", "Wallet address is empty, skipping wallet data loading")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                // Load wallet balance and staking info concurrently
                val balanceJob = launch {
                    try {
                        // Try to get real Solana wallet balance first
                        if (walletAddress.isBlank()) {
                            throw IllegalArgumentException("Wallet address cannot be empty")
                        }

                        // Convert wallet address string to SolanaPublicKey
                        val publicKey = SolanaPublicKey.from(walletAddress)

                        // Get balance from Solana network
                        val lamports = solanaAccountBalanceUseCase.getBalance(publicKey)

                        // Convert lamports to SOL (1 SOL = 1,000,000,000 lamports)
                        val solBalance = lamports / 1_000_000_000.0

                        val usdcBalance = solanaTokenBalanceUseCase.getBalanceByOwnerAndMint(publicKey).toDouble() / 1_000_000_000.0

                        val walletBalance = WalletBalance(
                            usdcBalance = usdcBalance,
                            solBalance = solBalance,
                            stakedAmount = 0.0, // TODO: Implement staking amount fetching
                            totalValue = solBalance,
                            lastUpdated = System.currentTimeMillis()
                        )

                        Log.d("ProfileViewModel", "Real wallet balance loaded: ${walletBalance.totalValue} SOL")
                        _uiState.value = _uiState.value.copy(walletBalance = walletBalance)
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Exception getting real wallet balance: ${e.message}")
                        // Fallback to mock data
                        getWalletBalanceUseCase(walletAddress).catch { fallbackError ->
                            Log.e("ProfileViewModel", "Fallback wallet balance flow error: ${fallbackError.message}")
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load wallet balance: ${fallbackError.message}"
                            )
                        }.collect { fallbackResult ->
                            fallbackResult.fold(
                                onSuccess = { balance ->
                                    _uiState.value = _uiState.value.copy(walletBalance = balance)
                                },
                                onFailure = { fallbackError ->
                                    Log.e(
                                        "ProfileViewModel",
                                        "Failed to load fallback wallet balance: ${fallbackError.message}"
                                    )
                                    _uiState.value = _uiState.value.copy(
                                        error = "Failed to load wallet balance: ${fallbackError.message}"
                                    )
                                }
                            )
                        }
                    }
                }

                val stakingJob = launch {
                    getStakingInfoUseCase(walletAddress).catch { e ->
                        Log.e("ProfileViewModel", "Staking info flow error: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load staking info: ${e.message}"
                        )
                    }.collect { result ->
                        result.fold(
                            onSuccess = { stakingInfo ->
                                _uiState.value = _uiState.value.copy(stakingInfo = stakingInfo)
                            },
                            onFailure = { error ->
                                Log.e("ProfileViewModel", "Failed to load staking info: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to load staking info: ${error.message}"
                                )
                            }
                        )
                    }
                }

                // Wait for both jobs to complete, then set loading to false
                balanceJob.join()
                stakingJob.join()
                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in loadWalletData: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load wallet data: ${e.message}"
                )
            }
        }
    }

    private fun loadUserAddresses() {
        viewModelScope.launch {
            try {
                addressLocalDataSource.getUserAddresses().catch { e ->
                    Log.e("ProfileViewModel", "User addresses flow error: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load addresses: ${e.message}"
                    )
                }.collect { addresses ->
                    _uiState.value = _uiState.value.copy(
                        userAddresses = addresses,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in loadUserAddresses: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load user addresses: ${e.message}"
                )
            }
        }
    }

    fun connectWallet(activityResultSender: ActivityResultSender) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                when (val result = solanaWalletConnectUseCase.connect(activityResultSender)) {
                    is WalletConnectResult.Success -> {
                        val user = result.user
                        Log.d("ProfileViewModel", "Wallet connection successful for user: ${user.id}")
                        _uiState.value = _uiState.value.copy(
                            isWalletConnected = true,
                            user = user
                        )
                        // Load wallet data for the connected user
                        user.walletAddress?.let { address ->
                            loadWalletData(address)
                        } ?: run {
                            // If no wallet address, set loading to false
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        loadUserAddresses()
                    }

                    is WalletConnectResult.Error -> {
                        Log.e("ProfileViewModel", "Wallet connection error: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }

                    is WalletConnectResult.NoWalletFound -> {
                        Log.w("ProfileViewModel", "No Solana wallet found on device")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No Solana wallet found. Please install a compatible wallet app."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in connectWallet: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to connect wallet: ${e.message}"
                )
            }
        }
    }

    fun disconnectWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                solanaWalletConnectUseCase.disconnect()
                _uiState.value = ProfileUiState() // Reset to initial state
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in disconnectWallet: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to disconnect wallet: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadProfileData()
    }

    fun getAddressById(addressId: String): UserAddress? {
        return _uiState.value.userAddresses.find { it.id == addressId }
    }
    
    /**
     * Save or update user address
     */
    fun saveAddress(address: UserAddress, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                addressLocalDataSource.saveAddress(address).fold(
                    onSuccess = { savedAddress ->
                        Log.d("ProfileViewModel", "Address saved successfully: ${savedAddress.id}")
                        // Reload addresses to update UI
                        loadUserAddresses()
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("ProfileViewModel", "Failed to save address: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to save address: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in saveAddress: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save address: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Delete user address
     */
    fun deleteAddress(addressId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                addressLocalDataSource.deleteAddress(addressId).fold(
                    onSuccess = {
                        Log.d("ProfileViewModel", "Address deleted successfully: $addressId")
                        // Reload addresses to update UI
                        loadUserAddresses()
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("ProfileViewModel", "Failed to delete address: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete address: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in deleteAddress: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete address: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Set address as default
     */
    fun setDefaultAddress(addressId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                addressLocalDataSource.setDefaultAddress(addressId).fold(
                    onSuccess = {
                        Log.d("ProfileViewModel", "Default address set successfully: $addressId")
                        // Reload addresses to update UI
                        loadUserAddresses()
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("ProfileViewModel", "Failed to set default address: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to set default address: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in setDefaultAddress: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to set default address: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Request USDC faucet
     */
    fun requestUsdcFaucet(activityResultSender: ActivityResultSender, solAmount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val user = _uiState.value.user
            if (user?.walletAddress == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Wallet not connected"
                )
                return@launch
            }
            
            try {
                requestUsdcFaucetUseCase(user.walletAddress, activityResultSender, solAmount)
                    .collect { result ->
                        result.fold(
                            onSuccess = { signature ->
                                Log.d("ProfileViewModel", "USDC faucet successful: $signature")
                                // Refresh wallet balance after successful faucet
                                loadWalletData(user.walletAddress)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                            },
                            onFailure = { error ->
                                Log.e("ProfileViewModel", "USDC faucet failed: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "USDC faucet failed: ${error.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in requestUsdcFaucet: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "USDC faucet failed: ${e.message}"
                )
            }
        }
    }
}
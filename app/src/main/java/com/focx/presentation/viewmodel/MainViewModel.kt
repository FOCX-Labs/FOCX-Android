package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Connected
import com.focx.domain.usecase.SolanaWalletConnectUseCase
import com.focx.domain.usecase.SolanaWalletPersistenceUseCase
import com.focx.domain.usecase.WalletConnectResult
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainViewState(
    val isLoading: Boolean = false,
    val canTransact: Boolean = false,
    val userAddress: String = "",
    val userLabel: String = "",
    val walletFound: Boolean = true,
    val snackbarMessage: String? = null,
    val isConnected: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val persistenceUseCase: SolanaWalletPersistenceUseCase,
    private val solanaWalletConnectUseCase: SolanaWalletConnectUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainViewState())
    val viewState: StateFlow<MainViewState> = _state.asStateFlow()

    private fun MainViewState.updateViewState() {
        _state.update { this }
    }

    fun loadConnection() {
        viewModelScope.launch {
            val persistedConn = solanaWalletConnectUseCase.loadConnection()

            if (persistedConn is Connected) {
                _state.value.copy(
                    isLoading = true,
                    canTransact = true,
                    userAddress = persistedConn.publicKey.base58(),
                    userLabel = persistedConn.accountLabel,
                    isConnected = true
                ).updateViewState()

                _state.value.copy(
                    isLoading = false,
                    snackbarMessage = "✅ | Successfully auto-connected to: \n" + persistedConn.publicKey.base58() + "."
                ).updateViewState()
            } else {
                _state.value.copy(
                    isConnected = false,
                    canTransact = false,
                    userAddress = "",
                    userLabel = ""
                ).updateViewState()
            }
        }
    }

    fun connect(sender: ActivityResultSender) {
        viewModelScope.launch {
            _state.value.copy(isLoading = true).updateViewState()

            when (val result = solanaWalletConnectUseCase.connect(sender)) {
                is WalletConnectResult.Success -> {
                    val connection = persistenceUseCase.getWalletConnection()
                    if (connection is Connected) {
                        _state.value.copy(
                            isLoading = false,
                            canTransact = true,
                            userAddress = connection.publicKey.base58(),
                            userLabel = connection.accountLabel,
                            isConnected = true,
                            snackbarMessage = "✅ | Successfully connected to wallet."
                        ).updateViewState()
                    }
                }

                is WalletConnectResult.Error -> {
                    _state.value.copy(
                        isLoading = false,
                        canTransact = false,
                        isConnected = false,
                        snackbarMessage = "❌ | ${result.message}"
                    ).updateViewState()
                }

                is WalletConnectResult.NoWalletFound -> {
                    _state.value.copy(
                        isLoading = false,
                        walletFound = false,
                        canTransact = false,
                        isConnected = false,
                        snackbarMessage = "❌ | No wallet found."
                    ).updateViewState()
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            solanaWalletConnectUseCase.disconnect()
            MainViewState().copy(
                snackbarMessage = "✅ | Disconnected from wallet."
            ).updateViewState()
        }
    }

    fun clearSnackBar() {
        _state.value.copy(
            snackbarMessage = null
        ).updateViewState()
    }
}
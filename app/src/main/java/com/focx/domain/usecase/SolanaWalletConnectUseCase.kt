package com.focx.domain.usecase

import com.focx.domain.entity.Connected
import com.focx.domain.entity.User
import com.focx.utils.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.common.signin.SignInWithSolana
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject
import javax.inject.Singleton

sealed class WalletConnectResult {
    data class Success(val user: User) : WalletConnectResult()
    data class Error(val message: String) : WalletConnectResult()
    object NoWalletFound : WalletConnectResult()
}

class SolanaWalletConnectUseCase @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val persistenceUseCase: SolanaWalletPersistenceUseCase,
    private val loginWithWalletUseCase: LoginWithWalletUseCase
) {

    suspend fun connect(activityResultSender: ActivityResultSender): WalletConnectResult {
        return try {
            // Clear any existing connection and cached auth token before connecting
            // This ensures we get fresh authentication from the current Solflare account
            Log.d("SolanaWalletConnectUseCase", "Clearing existing connection before connecting")
            disconnect()
            
            // Clear the wallet adapter's cached auth token
            walletAdapter.authToken = null
            
            when (val result = walletAdapter.signIn(
                activityResultSender,
                SignInWithSolana.Payload("focx.com", "Sign in to Focx")
            )) {
                is TransactionResult.Success -> {
                    val currentConn = Connected(
                        SolanaPublicKey(result.authResult.publicKey),
                        result.authResult.accountLabel ?: "",
                        result.authResult.authToken
                    )

                    Log.d("SolanaWalletConnectUseCase", "New connection established for address: ${currentConn.publicKey.base58()}")

                    persistenceUseCase.persistConnection(
                        currentConn.publicKey, currentConn.accountLabel, currentConn.authToken
                    )

                    // Login with wallet address
                    val loginResult = loginWithWalletUseCase(currentConn.publicKey.base58())
                    if (loginResult.isSuccess) {
                        WalletConnectResult.Success(loginResult.getOrThrow())
                    } else {
                        WalletConnectResult.Error("Failed to login with wallet: ${loginResult.exceptionOrNull()?.message}")
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    WalletConnectResult.NoWalletFound
                }

                is TransactionResult.Failure -> {
                    WalletConnectResult.Error("Failed connecting to wallet: ${result.e.message}")
                }
            }
        } catch (e: TimeoutCancellationException) {
            WalletConnectResult.Error("Wallet connection timeout, please retry")
        }
    }

    fun disconnect() {
        Log.d("SolanaWalletConnectUseCase", "Disconnecting wallet and clearing cached data")
        persistenceUseCase.clearConnection()
        persistenceUseCase.forceClearMemoryConnection()
        // Clear the wallet adapter's cached auth token
        walletAdapter.authToken = null
    }

    fun getStoredConnection(): com.focx.domain.entity.WalletConnection {
        return persistenceUseCase.getWalletConnection()
    }

    fun loadConnection(): com.focx.domain.entity.WalletConnection {
        val persistedConn = persistenceUseCase.getWalletConnection()

        if (persistedConn is Connected) {
            walletAdapter.authToken = persistedConn.authToken
            Log.d("loadConnection", "Loaded connection success: ${persistedConn.authToken}")
        }

        return persistedConn
    }
}
package com.focx.domain.usecase

import com.focx.domain.entity.Connected
import com.focx.domain.entity.User
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

@Singleton
class SolanaWalletConnectUseCase @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val persistenceUseCase: SolanaWalletPersistenceUseCase,
    private val loginWithWalletUseCase: LoginWithWalletUseCase
) {

    suspend fun connect(activityResultSender: ActivityResultSender): WalletConnectResult {
        return try {
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
        persistenceUseCase.clearConnection()
    }

    fun getStoredConnection(): com.focx.domain.entity.WalletConnection {
        return persistenceUseCase.getWalletConnection()
    }
}
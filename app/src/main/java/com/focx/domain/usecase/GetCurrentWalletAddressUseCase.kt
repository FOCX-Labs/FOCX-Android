package com.focx.domain.usecase

import com.focx.domain.entity.Connected
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCurrentWalletAddressUseCase @Inject constructor(
    private val solanaWalletConnectUseCase: SolanaWalletConnectUseCase
) {
    /**
     * Get current connected wallet address
     *
     * @return Wallet address string if connected, null if not connected
     */
    fun execute(): String? {
        val connection = solanaWalletConnectUseCase.getStoredConnection()
        return when (connection) {
            is Connected -> connection.publicKey.base58()
            else -> null
        }
    }

    /**
     * Check if wallet is currently connected
     *
     * @return true if wallet is connected, false otherwise
     */
    fun isWalletConnected(): Boolean {
        return solanaWalletConnectUseCase.getStoredConnection() is Connected
    }
}
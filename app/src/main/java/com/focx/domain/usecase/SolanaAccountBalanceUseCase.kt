package com.focx.domain.usecase

import com.focx.core.network.NetworkConnectionManager
import com.focx.utils.Log
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaAccountBalanceUseCase @Inject constructor(
    private val networkConnectionManager: NetworkConnectionManager
) {
    private val TAG = SolanaAccountBalanceUseCase::class.simpleName

    suspend fun getBalance(address: SolanaPublicKey): Long {
        return try {
            val address58 = address.base58()
            val rpcResponse = networkConnectionManager.getSolanaRpcClient().getBalance(address)
            val balance = rpcResponse.result ?: 0L

            Log.d(TAG, "getBalance pubKey=${address58}, balance=$balance")
            balance
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching balance for account [${address.base58()}]: ${e.message}", e)
            throw InvalidAccountException("Could not fetch balance for account [${address.base58()}]: ${e.message}", e)
        }
    }

    class InvalidAccountException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
}
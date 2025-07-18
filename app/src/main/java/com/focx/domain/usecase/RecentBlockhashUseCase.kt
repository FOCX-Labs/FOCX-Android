package com.focx.domain.usecase

import com.focx.utils.Log
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.transaction.Blockhash
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentBlockhashUseCase @Inject constructor(
    private val solanaRpcClient: SolanaRpcClient
) {
    private val TAG = RecentBlockhashUseCase::class.simpleName

    suspend operator fun invoke(commitment: Commitment = Commitment.CONFIRMED): Blockhash =
        try {
            val response = solanaRpcClient.getLatestBlockhash(commitment)
            if (response.error != null) {
                throw BlockhashException("Could not fetch latest blockhash: ${response.error}")
            }
            val blockhashStr = response.result?.blockhash
                ?: throw BlockhashException("Could not fetch latest blockhash: UnknownError")
            Log.d(TAG, "getLatestBlockhash blockhash=$blockhashStr")
            Blockhash.from(blockhashStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest blockhash: ${e.message}", e)
            throw BlockhashException("Could not fetch latest blockhash: ${e.message}", e)
        }

    class BlockhashException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
}
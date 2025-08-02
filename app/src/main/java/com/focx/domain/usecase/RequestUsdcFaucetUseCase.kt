package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaFaucetDataSource
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RequestUsdcFaucetUseCase @Inject constructor(
    private val faucetDataSource: SolanaFaucetDataSource
) {
    suspend operator fun invoke(
        accountPublicKey: String,
        activityResultSender: ActivityResultSender,
        solAmount: Double
    ): Flow<Result<String>> {
        return faucetDataSource.requestUsdcFaucet(accountPublicKey, activityResultSender, solAmount)
    }
} 
package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaVaultDataSource
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RequestUnstakeUsdcUseCase @Inject constructor(
    private val vaultDataSource: SolanaVaultDataSource
) {
    suspend operator fun invoke(
        accountPublicKey: String,
        amount: ULong,
        activityResultSender: ActivityResultSender
    ): Flow<Result<String>> {
        return vaultDataSource.requestUnstake(accountPublicKey, amount, activityResultSender)
    }
} 
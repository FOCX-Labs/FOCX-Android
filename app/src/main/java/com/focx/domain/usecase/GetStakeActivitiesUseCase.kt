package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaVaultDataSource
import com.focx.domain.entity.StakeActivity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStakeActivitiesUseCase @Inject constructor(
    private val vaultDataSource: SolanaVaultDataSource
) {
    suspend operator fun invoke(accountPublicKey: String): Flow<Result<List<StakeActivity>>> {
        return vaultDataSource.getStakeActivities(accountPublicKey)
    }
} 
package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaVaultDataSource
import com.focx.domain.entity.VaultInfoWithStakers
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVaultInfoWithStakersUseCase @Inject constructor(
    private val vaultDataSource: SolanaVaultDataSource
) {
    suspend operator fun invoke(accountPublicKey: String): Flow<Result<VaultInfoWithStakers>> {
        return vaultDataSource.getVaultInfoWithStakers(accountPublicKey)
    }
}

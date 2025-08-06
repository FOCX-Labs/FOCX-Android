package com.focx.domain.usecase

import com.focx.data.datasource.solana.SolanaVaultDataSource
import com.focx.domain.entity.Vault
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVaultInfoUseCase @Inject constructor(
    private val vaultDataSource: SolanaVaultDataSource
) {
    suspend operator fun invoke(accountPublicKey: String): Flow<Result<Vault>> {
        return vaultDataSource.getVaultInfo(accountPublicKey)
    }
} 
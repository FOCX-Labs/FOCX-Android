package com.focx.data.datasource.solana

import com.focx.domain.entity.VaultDepositor
import com.focx.domain.entity.Transaction
import com.focx.domain.entity.WalletBalance
import com.focx.domain.repository.IWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SolanaWalletRepository @Inject constructor(
    private val vaultDataSource: SolanaVaultDataSource
) : IWalletRepository {
    override suspend fun getBalance(address: String): Flow<WalletBalance> {
        throw NotImplementedError("getBalance is not implemented yet.")
    }

    override suspend fun getTransactionHistory(address: String): Flow<List<Transaction>> {
        throw NotImplementedError("getTransactionHistory is not implemented yet.")
    }

    override suspend fun sendTransaction(toAddress: String, amount: Double, currency: String): Result<String> {
        throw NotImplementedError("sendTransaction is not implemented yet.")
    }

    override suspend fun getStakingInfo(address: String): Flow<VaultDepositor?> {
        // Only emit the data part, not the Result wrapper
        return vaultDataSource.getStakingInfo(address).map { result ->
            result.getOrNull()
        }
    }

    override suspend fun stakeTokens(amount: Double): Result<String> {
        throw NotImplementedError("stakeTokens is not implemented yet.")
    }

    override suspend fun unstakeTokens(amount: Double): Result<String> {
        throw NotImplementedError("unstakeTokens is not implemented yet.")
    }

    override suspend fun claimRewards(): Result<String> {
        throw NotImplementedError("claimRewards is not implemented yet.")
    }

    override suspend fun connectWallet(): Result<String> {
        throw NotImplementedError("connectWallet is not implemented yet.")
    }

    override suspend fun disconnectWallet(): Result<Unit> {
        throw NotImplementedError("disconnectWallet is not implemented yet.")
    }

    override suspend fun isWalletConnected(): Boolean {
        throw NotImplementedError("isWalletConnected is not implemented yet.")
    }
}
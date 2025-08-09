package com.focx.domain.repository

import com.focx.domain.entity.VaultDepositor
import com.focx.domain.entity.Transaction
import com.focx.domain.entity.WalletBalance
import kotlinx.coroutines.flow.Flow

interface IWalletRepository {
    suspend fun getBalance(address: String): Flow<WalletBalance>
    suspend fun getTransactionHistory(address: String): Flow<List<Transaction>>
    suspend fun sendTransaction(toAddress: String, amount: Double, currency: String): Result<String>
    suspend fun getStakingInfo(address: String): Flow<VaultDepositor?>
    suspend fun stakeTokens(amount: Double): Result<String>
    suspend fun unstakeTokens(amount: Double): Result<String>
    suspend fun claimRewards(): Result<String>
    suspend fun connectWallet(): Result<String>
    suspend fun disconnectWallet(): Result<Unit>
    suspend fun isWalletConnected(): Boolean
}
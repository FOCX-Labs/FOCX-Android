package com.focx.data.datasource.mock

import com.focx.domain.entity.StakingInfo
import com.focx.domain.entity.Transaction
import com.focx.domain.entity.TransactionStatus
import com.focx.domain.entity.TransactionType
import com.focx.domain.entity.WalletBalance
import com.focx.domain.repository.IWalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockWalletDataSource @Inject constructor() : IWalletRepository {

    private var isWalletConnected = false
    private var walletAddress: String? = null

    override suspend fun getBalance(address: String): Flow<WalletBalance> = flow {
        delay(100) // Reduce delay for better responsiveness
        emit(getSampleWalletBalance())
    }

    override suspend fun getTransactionHistory(address: String): Flow<List<Transaction>> = flow {
        delay(150) // Reduce delay for better responsiveness
        emit(getSampleTransactions())
    }

    override suspend fun getStakingInfo(address: String): Flow<StakingInfo> = flow {
        delay(100) // Reduce delay for better responsiveness
        emit(getSampleStakingInfo())
    }

    override suspend fun stakeTokens(amount: Double): Result<String> {
        delay(200) // Reduce delay for better responsiveness
        return Result.success("0x${System.currentTimeMillis().toString(16)}")
    }

    override suspend fun unstakeTokens(amount: Double): Result<String> {
        delay(200) // Reduce delay for better responsiveness
        return Result.success("0x${System.currentTimeMillis().toString(16)}")
    }

    override suspend fun claimRewards(): Result<String> {
        delay(250) // Reduce delay for better responsiveness
        return Result.success("0x${System.currentTimeMillis().toString(16)}")
    }

    override suspend fun sendTransaction(toAddress: String, amount: Double, currency: String): Result<String> {
        delay(200) // Reduce delay for better responsiveness
        return Result.success("0x${System.currentTimeMillis().toString(16)}")
    }

    override suspend fun connectWallet(): Result<String> {
        // Remove delay and fix implementation for mock testing
        isWalletConnected = true
        walletAddress = "mock_wallet_address_${System.currentTimeMillis()}"
        return Result.success(walletAddress!!)
    }

    override suspend fun disconnectWallet(): Result<Unit> {
        delay(50) // Minimal delay for disconnect
        isWalletConnected = false
        walletAddress = null
        return Result.success(Unit)
    }

    override suspend fun isWalletConnected(): Boolean {
        return isWalletConnected
    }

    // Mock methods for testing
    fun mockConnectWallet() {
        isWalletConnected = true
    }

    fun mockDisconnectWallet() {
        isWalletConnected = false
    }

    private fun getSampleWalletBalance(): WalletBalance {
        return WalletBalance(
            usdcBalance = 12850.50,
            solBalance = 2.5,
            stakedAmount = 2570.25,
            totalValue = 15420.75
        )
    }

    private fun getSampleStakingInfo(): StakingInfo {
        return StakingInfo(
            totalStaked = 2570.25,
            availableToStake = 12850.50,
            stakingRewards = 125.75,
            stakingApr = 8.5,
            unstakingPeriod = 7, // 7 days
            lastStakeDate = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
            nextRewardDate = System.currentTimeMillis() + (4 * 24 * 60 * 60 * 1000) // 4 days from now
        )
    }

    private fun getSampleTransactions(): List<Transaction> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            Transaction(
                id = "tx_001",
                type = TransactionType.SEND,
                amount = 150.0,
                currency = "USDC",
                fromAddress = "mock_address",
                toAddress = "0x123456789abcdef0123456789abcdef012345678",
                transactionHash = "0xa1b2c3d4e5f6789012345678901234567890abcdef",
                status = TransactionStatus.CONFIRMED,
                timestamp = currentTime - (2 * 60 * 60 * 1000), // 2 hours ago
                gasUsed = 21000.0,
                gasFee = 0.005
            ),
            Transaction(
                id = "tx_002",
                type = TransactionType.RECEIVE,
                amount = 500.0,
                currency = "USDC",
                fromAddress = "0x987654321fedcba0987654321fedcba098765432",
                toAddress = "mock_address",
                transactionHash = "0xb2c3d4e5f6789012345678901234567890abcdef1",
                status = TransactionStatus.CONFIRMED,
                timestamp = currentTime - (6 * 60 * 60 * 1000), // 6 hours ago
                gasUsed = 21000.0,
                gasFee = 0.003
            ),
            Transaction(
                id = "tx_003",
                type = TransactionType.STAKE,
                amount = 1000.0,
                currency = "USDC",
                fromAddress = "mock_address",
                toAddress = "staking_contract",
                transactionHash = "0xc3d4e5f6789012345678901234567890abcdef12",
                status = TransactionStatus.CONFIRMED,
                timestamp = currentTime - (24 * 60 * 60 * 1000), // 1 day ago
                gasUsed = 45000.0,
                gasFee = 0.012
            ),
            Transaction(
                id = "tx_004",
                type = TransactionType.REWARD,
                amount = 25.75,
                currency = "USDC",
                fromAddress = "rewards_contract",
                toAddress = "mock_address",
                transactionHash = "0xd4e5f6789012345678901234567890abcdef123",
                status = TransactionStatus.CONFIRMED,
                timestamp = currentTime - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                gasUsed = 21000.0,
                gasFee = 0.002
            )
        )
    }
}
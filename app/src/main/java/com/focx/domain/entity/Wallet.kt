package com.focx.domain.entity

data class WalletBalance(
    val usdcBalance: Double,
    val solBalance: Double,
    val stakedAmount: Double,
    val totalValue: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val fromAddress: String,
    val toAddress: String,
    val transactionHash: String,
    val status: TransactionStatus,
    val timestamp: Long,
    val gasUsed: Double? = null,
    val gasFee: Double? = null
)

enum class TransactionType {
    SEND,
    RECEIVE,
    STAKE,
    UNSTAKE,
    PURCHASE,
    SALE,
    REWARD
}

enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    FAILED
}

data class StakingInfo(
    val totalStaked: Double,
    val availableToStake: Double,
    val stakingRewards: Double,
    val stakingApr: Double,
    val unstakingPeriod: Int, // days
    val lastStakeDate: Long,
    val nextRewardDate: Long
)
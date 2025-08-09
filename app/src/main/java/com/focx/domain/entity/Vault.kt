package com.focx.domain.entity

import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class UnstakeRequest(
    val shares: ULong, // Number of shares to unstake
    val requestTime: Long, // When the unstake request was made
    val assetPerShareAtRequest: ULong, // Asset amount per share at request time (scaled by PRECISION)
    val assetPerShareAtRequest2: ULong // Asset amount per share at request time (scaled by PRECISION)
)

@Serializable
data class UpdateVaultConfigParams(
    val unstakeLockupPeriod: Long?, // Option<i64>
    val managementFee: ULong?, // Option<u64>
    val minStakeAmount: ULong?, // Option<u64>
    val maxTotalAssets: ULong?, // Option<u64>
    val isPaused: Boolean?, // Option<bool>
    val platformAccount: SolanaPublicKey? // Option<pubkey>
)

@Serializable
data class Vault(
//    val discriminator: Long,
//    val name: ByteArray = ByteArray(32), // [u8; 32] - The name of the vault
    val pubkey: SolanaPublicKey, // The vault's pubkey
    val owner: SolanaPublicKey, // The owner/admin of the vault
    val platformAccount: SolanaPublicKey, // The platform account for receiving 50% of rewards
    val tokenMint: SolanaPublicKey, // The token mint for staking
    val vaultTokenAccount: SolanaPublicKey, // The vault token account (main asset pool)
    val totalShares: ULong, // Total supply of shares
    val totalAssets: ULong, // Total assets in the vault
    val totalRewards: ULong, // Total rewards distributed
    val rewardsPerShare: ULong, // Rewards per share (scaled by SHARE_PRECISION)
    val rewardsPerShare2: ULong,
    val lastRewardsUpdate: Long, // Last time rewards were updated
    val unstakeLockupPeriod: Long, // Unstake lockup period in seconds
    val managementFee: ULong, // Platform share percentage for add_rewards (in basis points)
    val minStakeAmount: ULong, // Minimum stake amount
    val maxTotalAssets: ULong, // Maximum total assets
    val isPaused: Boolean, // Whether the vault is paused
    val createdAt: Long, // Vault creation timestamp
    val sharesBase: UInt, // Shares base for rebase tracking
    val rebaseVersion: UInt, // Current rebase version for tracking
    val ownerShares: ULong, // Owner shares (owner as a normal depositor)
    val pendingUnstakeShares: ULong, // Total shares pending unstake (not participating in rewards)
    val reservedAssets: ULong, // Assets reserved for pending unstake requests (frozen assets)
    val bump: UByte, // Bump seed for PDA
//    val reserved: ByteArray // [u8; 16] - Reserved for future use
)

/**
 * Enhanced vault info that includes total stakers without modifying the original Vault contract structure
 */
@Serializable
data class VaultInfoWithStakers(
    val vault: Vault,
    val totalStakers: Int
)

@Serializable
data class VaultDepositor(
    val discriminator: Long,
    val vault: SolanaPublicKey, // The vault this depositor belongs to
    val authority: SolanaPublicKey, // The depositor's authority
    val shares: ULong, // The depositor's shares
    val rewardsDebt: ULong, // The depositor's rewards debt (for reward calculation)
    val rewardsDebt2: ULong, // The depositor's rewards debt (for reward calculation)
    val lastRewardsClaim: Long, // Last time rewards were claimed
    val unstakeRequest: UnstakeRequest, // Unstake request
    val totalStaked: ULong, // Total amount staked
    val totalUnstaked: ULong, // Total amount unstaked
    val totalRewardsClaimed: ULong, // Total rewards claimed
    val createdAt: Long, // When the depositor was created
    val lastRebaseVersion: UInt, // Last rebase version user has synced with
    val lastStakeTime: Long, // Last time user staked (for MEV protection)
)


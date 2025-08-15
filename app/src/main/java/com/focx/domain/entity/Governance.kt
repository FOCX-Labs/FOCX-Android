package com.focx.domain.entity

import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class Proposal(
    val discriminator: Long,
    /// Proposal ID
    val id: ULong,
    /// Proposal proposer
    val proposer: SolanaPublicKey,
    /// Proposal type
    val proposalType: ProposalType,
    /// Proposal title
    val title: String,
    /// Proposal description
    val description: String,
    /// Deposit amount
    val depositAmount: ULong,
    /// Creation time
    val createdAt: Long,
    /// Voting start time
    val votingStart: Long,
    /// Voting end time
    val votingEnd: Long,
    /// Proposal status
    val status: ProposalStatus,
    /// Yes votes
    val yesVotes: ULong,
    /// No votes
    val noVotes: ULong,
    /// Abstain votes
    val abstainVotes: ULong,
    /// Veto votes
    val vetoVotes: ULong,
    /// Total votes
    val totalVotes: ULong,
    /// Execution data
    val executionData: ExecutionData?,
    /// Execution result
    val executionResult: String?,
    /// PDA bump
    val bump: UByte
)

enum class ProposalType {
    /// Illegal product slash
    SLASH,
    /// Trade dispute arbitration
    DISPUTE,
    /// Rule update
    RULE_UPDATE,
    /// Configuration update
    CONFIG_UPDATE
}

@Serializable
data class ExecutionData(
    /// Execution timestamp
    val executedAt: Long,
    /// Executor address
    val executor: SolanaPublicKey,
    /// Execution transaction hash
    val transactionHash: String
)

enum class ProposalStatus {
    /// Voting in progress
    PENDING,
    /// Passed
    PASSED,
    /// Rejected
    REJECTED,
    /// Vetoed
    VETOED,
    /// Executed
    EXECUTED
}

data class Vote(
    val id: String,
    val proposalId: String,
    val voterId: String,
    val voterName: String,
    val voteType: VoteType,
    val votingPower: Double,
    val timestamp: Long,
    val transactionHash: String
)

enum class VoteType {
    YES,           // 0
    NO,            // 1
    ABSTAIN,       // 2
    NO_WITH_VETO   // 3
}

data class GovernanceStats(
    val totalProposals: ULong,
    val totalVotingPower: Double,
    val canVote: Boolean = false
)

data class Dispute(
    val id: String,
    val title: String,
    val buyer: String,
    val order: String,
    val amount: String,
    val submitted: String,
    val status: DisputeStatus,
    val daysRemaining: Int,
    val evidenceSummary: String,
    val communityVoting: CommunityVoting?,
    val resolution: String? = null
)

data class CommunityVoting(
    val buyerFavor: Int,
    val sellerFavor: Int
)

enum class DisputeStatus {
    UNDER_REVIEW, VOTING, RESOLVED
}

data class PlatformRule(
    val category: String,
    val rules: List<String>
)

// Governance instruction data structures
@Serializable
data class CreateProposalArgs(
    val title: String,
    val description: String,
    val proposalType: ProposalType,
    val executionData: ExecutionData?,
    val customDepositRaw: ULong?
)

@Serializable
data class VoteOnProposalArgs(
    val proposalId: ULong,
    val voteType: VoteType
)

@Serializable
data class InitiateDisputeArgs(
    val orderId: String
)

@Serializable
data class FinalizeProposalArgs(
    val proposalId: ULong
)

@Serializable
data class GovernanceConfig(
    val discriminator: Long,
    /// Administrator address
    val authority: SolanaPublicKey,
    /// Committee token mint address (fixed to specified SPL Token)
    val committeeTokenMint: SolanaPublicKey,
    /// Committee member address array (maximum 10 members)
    val committeeMembers: List<SolanaPublicKey?>,
    /// Committee member count
    val committeeMemberCount: UByte,
    /// Proposal deposit amount (100 USDC)
    val proposalDeposit: ULong,
    /// Voting period (14 days, in seconds)
    val votingPeriod: ULong,
    /// Participation threshold requirement (40% = 4000 basis points)
    val participationThreshold: UShort,
    /// Approval threshold requirement (50% = 5000 basis points)
    val approvalThreshold: UShort,
    /// Veto threshold (30% = 3000 basis points)
    val vetoThreshold: UShort,
    /// Committee fee rate (10% = 1000 basis points)
    val feeRate: UShort,
    /// Total voting power
    val totalVotingPower: ULong,
    /// Proposal counter
    val proposalCounter: ULong,
    /// Creation time
    val createdAt: Long,
    /// Last update time
    val updatedAt: Long,
    /// Test mode flag
    val testMode: Boolean,
    /// PDA bump
    val bump: UByte
)
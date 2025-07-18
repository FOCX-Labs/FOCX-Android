package com.focx.domain.entity

data class Proposal(
    val id: String,
    val title: String,
    val description: String,
    val proposer: String,
    val proposerName: String,
    val securityDeposit: Double,
    val status: ProposalStatus,
    val votesFor: Int,
    val votesAgainst: Int,
    val totalVotes: Int,
    val votingStartTime: Long,
    val votingEndTime: Long,
    val executionTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val category: ProposalCategory
)

enum class ProposalStatus {
    DRAFT,
    ACTIVE,
    PASSED,
    REJECTED,
    EXECUTED,
    CANCELLED
}

enum class ProposalCategory {
    PLATFORM_UPGRADE,
    FEE_ADJUSTMENT,
    GOVERNANCE_CHANGE,
    TREASURY_MANAGEMENT,
    SECURITY_UPDATE,
    OTHER
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
    FOR,
    AGAINST,
    ABSTAIN
}

data class GovernanceStats(
    val activeProposals: Int,
    val totalProposals: Int,
    val totalVotes: Int,
    val passRate: Double,
    val totalVotingPower: Double,
    val participationRate: Double
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
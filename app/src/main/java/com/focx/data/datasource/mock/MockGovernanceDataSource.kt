package com.focx.data.datasource.mock

import com.focx.domain.entity.CommunityVoting
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.DisputeStatus
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.ProposalCategory
import com.focx.domain.entity.ProposalStatus
import com.focx.domain.entity.Vote
import com.focx.domain.entity.VoteType
import com.focx.domain.repository.IGovernanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGovernanceDataSource @Inject constructor() : IGovernanceRepository {

    override suspend fun getProposals(): Flow<List<Proposal>> = flow {
        // Simulate API call
        kotlinx.coroutines.delay(500)
        emit(getSampleProposals())
    }

    override suspend fun getProposalById(id: String): Proposal? {
        kotlinx.coroutines.delay(300)
        return getSampleProposals().find { it.id == id }
    }

    override suspend fun getActiveProposals(): Flow<List<Proposal>> = flow {
        kotlinx.coroutines.delay(400)
        emit(getSampleProposals().filter { it.status == ProposalStatus.ACTIVE })
    }

    override suspend fun getGovernanceStats(): Flow<GovernanceStats> = flow {
        kotlinx.coroutines.delay(300)
        emit(
            GovernanceStats(
                activeProposals = 12,
                totalProposals = 45,
                totalVotes = 2456,
                passRate = 0.943,
                totalVotingPower = 1250.0,
                participationRate = 0.78
            )
        )
    }

    override suspend fun createProposal(proposal: Proposal): Result<Proposal> {
        kotlinx.coroutines.delay(1500)
        // Simulate successful creation
        return Result.success(proposal.copy(id = System.currentTimeMillis().toString()))
    }

    override suspend fun voteOnProposal(proposalId: String, voteType: VoteType): Result<Vote> {
        kotlinx.coroutines.delay(1000)
        val vote = Vote(
            id = System.currentTimeMillis().toString(),
            proposalId = proposalId,
            voterId = "current_user_id",
            voterName = "Current User",
            voteType = voteType,
            votingPower = 10.0,
            timestamp = System.currentTimeMillis(),
            transactionHash = "0x${System.currentTimeMillis().toString(16)}"
        )
        return Result.success(vote)
    }

    override suspend fun getVotesByProposal(proposalId: String): Flow<List<Vote>> = flow {
        kotlinx.coroutines.delay(400)
        emit(getSampleVotes().filter { it.proposalId == proposalId })
    }

    override suspend fun getVotesByUser(userId: String): Flow<List<Vote>> = flow {
        kotlinx.coroutines.delay(400)
        emit(getSampleVotes().filter { it.voterId == userId })
    }

    override suspend fun getUserVotingPower(userId: String): Double {
        kotlinx.coroutines.delay(200)
        return 10.0 // Mock voting power
    }

    override suspend fun getDisputes(): Flow<List<Dispute>> = flow {
        kotlinx.coroutines.delay(400) // Simulate network delay
        emit(mockDisputes)
    }

    override suspend fun getPlatformRules(): Flow<List<PlatformRule>> = flow {
        kotlinx.coroutines.delay(300) // Simulate network delay
        emit(mockPlatformRules)
    }

    override suspend fun voteOnDispute(disputeId: String, favorBuyer: Boolean): Result<Unit> {
        return try {
            kotlinx.coroutines.delay(200) // Simulate network delay
            // In a real implementation, this would update the dispute voting data
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getSampleProposals(): List<Proposal> {
        return mockGovernance
    }

    private fun getSampleVotes(): List<Vote> {
        return listOf(
            Vote(
                id = "vote_1",
                proposalId = "1",
                voterId = "user_1",
                voterName = "Jim",
                voteType = VoteType.FOR,
                votingPower = 15.0,
                timestamp = System.currentTimeMillis() - 3600000L,
                transactionHash = "0xabc123"
            ), Vote(
                id = "vote_2",
                proposalId = "1",
                voterId = "user_2",
                voterName = "Bob",
                voteType = VoteType.AGAINST,
                votingPower = 8.0,
                timestamp = System.currentTimeMillis() - 7200000L,
                transactionHash = "0xdef456"
            )
        )
    }

    companion object {
        val mockGovernance = listOf(
            Proposal(
                id = "1",
                title = "Slash Merchant for Selling Counterfeit Products",
                description = "Evidence shows merchant 0x1234...5678 has been selling counterfeit Apple products",
                proposer = "0x9876...4321",
                proposerName = "Security Deposit: 500 USDC",
                securityDeposit = 500.0,
                status = ProposalStatus.ACTIVE,
                votesFor = 1590,
                votesAgainst = 410,
                totalVotes = 2000,
                votingStartTime = System.currentTimeMillis() - 86400000L * 3, // 3 days ago
                votingEndTime = System.currentTimeMillis() + 86400000L * 4, // 4 days from now
                category = ProposalCategory.SECURITY_UPDATE
            ), Proposal(
                id = "2",
                title = "Update Platform Fee Structure",
                description = "Proposal to adjust transaction fees for better market competitiveness",
                proposer = "0x1111...2222",
                proposerName = "Platform Team",
                securityDeposit = 1000.0,
                status = ProposalStatus.ACTIVE,
                votesFor = 850,
                votesAgainst = 150,
                totalVotes = 1000,
                votingStartTime = System.currentTimeMillis() - 86400000L * 2,
                votingEndTime = System.currentTimeMillis() + 86400000L * 5,
                category = ProposalCategory.FEE_ADJUSTMENT
            )
        )

        val mockDisputes = listOf(
            Dispute(
                id = "1",
                title = "Product Not as Described - iPhone 15 Pro",
                buyer = "david_uMHi",
                order = "#3045",
                amount = "1,599 USDC",
                submitted = "2024-01-15",
                status = DisputeStatus.UNDER_REVIEW,
                daysRemaining = 5,
                evidenceSummary = "Buyer: Received iPhone 14, not iPhone 15 Pro as ordered\nIssue: Product shipped matches the listing, buyer may be confused",
                communityVoting = CommunityVoting(buyerFavor = 2567, sellerFavor = 1820)
            ), Dispute(
                id = "2",
                title = "Damaged During Shipping - MacBook Air",
                buyer = "mike_wVT1",
                order = "#3023",
                amount = "1,599 USDC",
                submitted = "2024-01-16",
                status = DisputeStatus.VOTING,
                daysRemaining = 3,
                evidenceSummary = "Buyer: MacBook screen cracked, poor packaging\nIssue: Properly packaged, damage occurred during transit",
                communityVoting = CommunityVoting(buyerFavor = 3267, sellerFavor = 965)
            ), Dispute(
                id = "3",
                title = "Non-Delivery - AirPods Pro Order",
                buyer = "TXI_JMC3",
                order = "#3018",
                amount = "299 USDC",
                submitted = "2024-01-14",
                status = DisputeStatus.RESOLVED,
                daysRemaining = 0,
                evidenceSummary = "Buyer: Never received package\nIssue: Package delivered to wrong address, seller provided refund",
                communityVoting = null,
                resolution = "Seller provided full refund. Case closed."
            )
        )

        private val mockPlatformRules = listOf(
            PlatformRule(
                "Product Listings", listOf(
                    "No counterfeit or replica products",
                    "Accurate product descriptions required",
                    "Real product images only",
                    "Proper categorization mandatory"
                )
            ), PlatformRule(
                "Trading Conduct", listOf(
                    "Honor all confirmed orders",
                    "Ship within stated timeframe",
                    "Provide tracking information",
                    "Respond to buyer inquiries within 24 hours"
                )
            ), PlatformRule(
                "Prohibited Items", listOf(
                    "Illegal substances and weapons", "Stolen goods", "Adult content", "Hazardous materials"
                )
            )
        )
    }
}
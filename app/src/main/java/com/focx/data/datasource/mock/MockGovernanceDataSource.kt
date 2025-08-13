package com.focx.data.datasource.mock

import com.focx.domain.entity.*
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGovernanceDataSource @Inject constructor() {

    companion object {
        val mockGovernance = listOf(
            Proposal(
                discriminator = 0L,
                id = 1UL,
                proposer = SolanaPublicKey.from("11111111111111111111111111111111"),
                proposalType = ProposalType.RULE_UPDATE,
                title = "Update Platform Fee Structure",
                description = "Proposal to adjust platform fees from 2.5% to 2.0% to increase competitiveness",
                depositAmount = 500UL,
                createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
                votingStart = System.currentTimeMillis() - 86400000,
                votingEnd = System.currentTimeMillis() + 1209600000, // 14 days from now
                status = ProposalStatus.PENDING,
                yesVotes = 1250UL,
                noVotes = 150UL,
                abstainVotes = 50UL,
                vetoVotes = 0UL,
                totalVotes = 1450UL,
                executionData = null,
                executionResult = null,
                bump = 0U
            ),
            Proposal(
                discriminator = 0L,
                id = 2UL,
                proposer = SolanaPublicKey.from("22222222222222222222222222222222"),
                proposalType = ProposalType.CONFIG_UPDATE,
                title = "Implement Enhanced Security Measures",
                description = "Add additional security layers including 2FA and enhanced encryption",
                depositAmount = 500UL,
                createdAt = System.currentTimeMillis() - 172800000, // 2 days ago
                votingStart = System.currentTimeMillis() - 172800000,
                votingEnd = System.currentTimeMillis() + 1036800000, // 12 days from now
                status = ProposalStatus.PENDING,
                yesVotes = 890UL,
                noVotes = 120UL,
                abstainVotes = 30UL,
                vetoVotes = 0UL,
                totalVotes = 1040UL,
                executionData = null,
                executionResult = null,
                bump = 0U
            )
        )

        val mockDisputes = listOf(
            Dispute(
                id = "dispute_001",
                title = "Order #12345 Dispute",
                buyer = "buyer123",
                order = "order_12345",
                amount = "299.99 USDC",
                submitted = "12/15/2024",
                status = DisputeStatus.UNDER_REVIEW,
                daysRemaining = 7,
                evidenceSummary = "Buyer claims item not as described",
                communityVoting = CommunityVoting(buyerFavor = 15, sellerFavor = 8)
            ),
            Dispute(
                id = "dispute_002",
                title = "Order #12346 Dispute",
                buyer = "buyer456",
                order = "order_12346",
                amount = "150.00 USDC",
                submitted = "12/14/2024",
                status = DisputeStatus.VOTING,
                daysRemaining = 3,
                evidenceSummary = "Seller claims buyer is trying to scam",
                communityVoting = CommunityVoting(buyerFavor = 5, sellerFavor = 22)
            )
        )

        val mockPlatformRules = listOf(
            PlatformRule(
                category = "Trading Rules",
                rules = listOf(
                    "All transactions must be completed within 7 days",
                    "Sellers must provide accurate product descriptions",
                    "Buyers must pay within 24 hours of order confirmation"
                )
            ),
            PlatformRule(
                category = "Dispute Resolution",
                rules = listOf(
                    "Disputes must be initiated within 48 hours of delivery",
                    "Both parties must provide evidence within 72 hours",
                    "Community voting determines final resolution"
                )
            )
        )
    }

    suspend fun voteOnProposal(
        proposalId: ULong,
        voteType: VoteType,
        voterPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            // Simulate network delay
            kotlinx.coroutines.delay(1000)
            
            // Find the proposal and update its vote counts
            val proposalIndex = mockGovernance.indexOfFirst { it.id == proposalId }
            if (proposalIndex == -1) {
                return Result.failure(Exception("Proposal not found"))
            }
            
            // In a real implementation, this would update the blockchain
            // For mock purposes, we just return success
            println("Mock vote cast: Proposal $proposalId, Vote: $voteType, Voter: ${voterPubKey.toString()}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to vote on proposal: ${e.message}"))
        }
    }
}

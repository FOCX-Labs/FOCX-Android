package com.focx.domain.repository

import com.focx.domain.entity.Dispute
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.ProposalType
import com.focx.domain.entity.Vote
import com.focx.domain.entity.VoteType
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.flow.Flow

interface IGovernanceRepository {
    suspend fun getProposals(): Flow<List<Proposal>>
    suspend fun getProposals(page: Int, pageSize: Int): Flow<List<Proposal>>
    suspend fun getProposalById(id: String): Proposal?
    suspend fun getActiveProposals(): Flow<List<Proposal>>
    suspend fun getActiveProposals(page: Int, pageSize: Int): Flow<List<Proposal>>
    suspend fun getGovernanceStats(currentUserPubKey: SolanaPublicKey? = null): Flow<GovernanceStats>
    suspend fun createProposal(
        title: String,
        description: String,
        proposalType: ProposalType,
        proposerPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit>

    suspend fun voteOnProposal(
        proposalId: ULong,
        voteType: VoteType,
        voterPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit>

    suspend fun finalizeProposal(
        proposalId: ULong,
        proposerPubKey: SolanaPublicKey,
        accountPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit>

    suspend fun getVotesByProposal(proposalId: String): Flow<List<Vote>>
    suspend fun getVotesByUser(userId: String): Flow<List<Vote>>
    suspend fun getUserVotingPower(userId: String): Double
    suspend fun getDisputes(): Flow<List<Dispute>>
    suspend fun getPlatformRules(): Flow<List<PlatformRule>>
    suspend fun voteOnDispute(disputeId: String, favorBuyer: Boolean): Result<Unit>
    suspend fun initiateDispute(
        orderId: String,
        buyerPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Dispute>
}
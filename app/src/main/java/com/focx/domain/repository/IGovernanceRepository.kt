package com.focx.domain.repository

import com.focx.domain.entity.Dispute
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.Vote
import com.focx.domain.entity.VoteType
import kotlinx.coroutines.flow.Flow

interface IGovernanceRepository {
    suspend fun getProposals(): Flow<List<Proposal>>
    suspend fun getProposalById(id: String): Proposal?
    suspend fun getActiveProposals(): Flow<List<Proposal>>
    suspend fun getGovernanceStats(): Flow<GovernanceStats>
    suspend fun createProposal(proposal: Proposal): Result<Proposal>
    suspend fun voteOnProposal(proposalId: String, voteType: VoteType): Result<Vote>
    suspend fun getVotesByProposal(proposalId: String): Flow<List<Vote>>
    suspend fun getVotesByUser(userId: String): Flow<List<Vote>>
    suspend fun getUserVotingPower(userId: String): Double
    suspend fun getDisputes(): Flow<List<Dispute>>
    suspend fun getPlatformRules(): Flow<List<PlatformRule>>
    suspend fun voteOnDispute(disputeId: String, favorBuyer: Boolean): Result<Unit>
}
package com.focx.domain.usecase

import com.focx.domain.entity.Dispute
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.repository.IGovernanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGovernanceDataUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {

    suspend fun getActiveProposals(): Flow<List<Proposal>> {
        return governanceRepository.getActiveProposals()
    }

    suspend fun getAllProposals(): Flow<List<Proposal>> {
        return governanceRepository.getProposals()
    }

    suspend fun getGovernanceStats(): Flow<GovernanceStats> {
        return governanceRepository.getGovernanceStats()
    }

    suspend fun getProposalById(id: String): Proposal? {
        return governanceRepository.getProposalById(id)
    }

    suspend fun getDisputes(): Flow<List<Dispute>> {
        return governanceRepository.getDisputes()
    }

    suspend fun getPlatformRules(): Flow<List<PlatformRule>> {
        return governanceRepository.getPlatformRules()
    }

    suspend fun voteOnDispute(disputeId: String, favorBuyer: Boolean): Result<Unit> {
        return governanceRepository.voteOnDispute(disputeId, favorBuyer)
    }
}
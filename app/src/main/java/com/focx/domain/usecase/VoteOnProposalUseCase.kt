package com.focx.domain.usecase

import com.focx.domain.entity.Vote
import com.focx.domain.entity.VoteType
import com.focx.domain.repository.IGovernanceRepository
import javax.inject.Inject

class VoteOnProposalUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {

    suspend fun execute(proposalId: String, voteType: VoteType): Result<Vote> {
        return try {
            // Check if user has voting power
            val votingPower = governanceRepository.getUserVotingPower("current_user_id")
            if (votingPower <= 0) {
                Result.failure(Exception("Insufficient voting power"))
            } else {
                governanceRepository.voteOnProposal(proposalId, voteType)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
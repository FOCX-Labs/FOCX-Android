package com.focx.domain.usecase

import com.focx.domain.entity.ProposalType
import com.focx.domain.repository.IGovernanceRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject

class CreateProposalUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {

    suspend fun execute(
        title: String, 
        description: String,
        proposalType: ProposalType,
        proposerPubKey: SolanaPublicKey, 
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            // Validate input parameters
            if (title.isBlank()) {
                Result.failure(Exception("Title cannot be empty"))
            } else if (description.isBlank()) {
                Result.failure(Exception("Description cannot be empty"))
            } else {
                governanceRepository.createProposal(title, description, proposalType, proposerPubKey, activityResultSender)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

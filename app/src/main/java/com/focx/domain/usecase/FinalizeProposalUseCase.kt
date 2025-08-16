package com.focx.domain.usecase

import com.focx.domain.repository.IGovernanceRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject

class FinalizeProposalUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {

    suspend fun execute(
        proposalId: ULong,
        proposerPubKey: SolanaPublicKey,
        accountPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            // Validate input parameters
            if (proposalId == 0UL) {
                Result.failure(Exception("Invalid proposal ID"))
            } else {
                governanceRepository.finalizeProposal(
                    proposalId,
                    proposerPubKey,
                    accountPubKey,
                    activityResultSender
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

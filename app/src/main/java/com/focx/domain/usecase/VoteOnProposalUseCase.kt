package com.focx.domain.usecase

import com.focx.domain.entity.VoteType
import com.focx.domain.repository.IGovernanceRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject

class VoteOnProposalUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {

    suspend fun execute(
        proposalId: ULong,
        voteType: VoteType,
        voterPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            // Check if user has voting power
//            val votingPower = governanceRepository.getUserVotingPower(voterPubKey.toString())
//            if (votingPower <= 0) {
//                Result.failure(Exception("Insufficient voting power"))
//            } else {
            governanceRepository.voteOnProposal(
                proposalId,
                voteType,
                voterPubKey,
                activityResultSender
            )
//            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.focx.domain.usecase

import com.focx.domain.entity.Dispute
import com.focx.domain.repository.IGovernanceRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject

class InitiateDisputeUseCase @Inject constructor(
    private val governanceRepository: IGovernanceRepository
) {
    suspend operator fun invoke(orderId: String, buyerPubKey: SolanaPublicKey, activityResultSender: ActivityResultSender): Result<Dispute> {
        return governanceRepository.initiateDispute(orderId, buyerPubKey, activityResultSender)
    }
}
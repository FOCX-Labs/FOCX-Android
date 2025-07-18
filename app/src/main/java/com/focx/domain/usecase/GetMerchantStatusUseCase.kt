package com.focx.domain.usecase

import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMerchantStatusUseCase @Inject constructor(
    private val merchantRepository: IMerchantRepository
) {
    /**
     * Get merchant registration status for a wallet address
     *
     * @param walletAddress Wallet public key to check
     * @return Flow of merchant status
     */
    suspend operator fun invoke(walletAddress: String): Flow<MerchantStatus> {
        if (walletAddress.isBlank()) {
            throw IllegalArgumentException("Wallet address cannot be empty")
        }

        return merchantRepository.getMerchantStatus(walletAddress)
    }
}
package com.focx.domain.usecase

import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.repository.IMerchantRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import javax.inject.Inject

class RegisterMerchantUseCase @Inject constructor(
    private val merchantRepository: IMerchantRepository
) {
    /**
     * Register merchant with atomic transaction
     * Validates input and calls Solana program register_merchant_atomic instruction
     *
     * @param name Merchant name (required)
     * @param description Merchant description (required)
     * @param merchantPublicKey Merchant wallet public key (required)
     * @param payerPublicKey Payer wallet public key (required)
     * @return Flow of registration result
     */
    suspend operator fun invoke(
        merchantRegistration: MerchantRegistration
    ): MerchantRegistrationResult {
        // Validate input parameters
        if (merchantRegistration.name.isBlank()) {
            throw IllegalArgumentException("Merchant name cannot be empty")
        }

        if (merchantRegistration.description.isBlank()) {
            throw IllegalArgumentException("Merchant description cannot be empty")
        }

        if (merchantRegistration.merchantPublicKey.isBlank()) {
            throw IllegalArgumentException("Merchant public key cannot be empty")
        }

        if (merchantRegistration.payerPublicKey.isBlank()) {
            throw IllegalArgumentException("Payer public key cannot be empty")
        }

        return merchantRepository.registerMerchantAtomic(merchantRegistration)
    }

    /**
     * Register merchant with atomic transaction using wallet interaction
     * Validates input and calls Solana program register_merchant_atomic instruction
     *
     * @param merchantRegistration Merchant registration data
     * @param activityResultSender Activity result sender for wallet interaction
     * @return Registration result
     */
    suspend operator fun invoke(
        merchantRegistration: MerchantRegistration,
        activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        // Validate input parameters
        if (merchantRegistration.name.isBlank()) {
            throw IllegalArgumentException("Merchant name cannot be empty")
        }

        if (merchantRegistration.description.isBlank()) {
            throw IllegalArgumentException("Merchant description cannot be empty")
        }

        if (merchantRegistration.merchantPublicKey.isBlank()) {
            throw IllegalArgumentException("Merchant public key cannot be empty")
        }

        if (merchantRegistration.payerPublicKey.isBlank()) {
            throw IllegalArgumentException("Payer public key cannot be empty")
        }

        return merchantRepository.registerMerchantAtomic(merchantRegistration, activityResultSender)
    }
}
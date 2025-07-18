package com.focx.data.datasource.mock

import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMerchantDataSource @Inject constructor() : IMerchantRepository {

    // Simulate registered merchants storage
    private val registeredMerchants = mutableMapOf<String, MerchantStatus>()

    override suspend fun registerMerchantAtomic(merchantRegistration: MerchantRegistration): MerchantRegistrationResult {
        return try {
            // Simulate network delay
            delay(1000)

            // Simulate transaction processing
            val transactionSignature = generateMockTransactionSignature()
            val merchantAccount = generateMockMerchantAccount(merchantRegistration.merchantPublicKey)

            // Store merchant status
            registeredMerchants[merchantRegistration.merchantPublicKey] = MerchantStatus(
                isRegistered = true,
                merchantAccount = merchantAccount,
                registrationDate = getCurrentTimestamp(),
                securityDeposit = merchantRegistration.securityDeposit,
                status = "ACTIVE"
            )

            MerchantRegistrationResult(
                success = true,
                transactionSignature = transactionSignature,
                merchantAccount = merchantAccount
            )
        } catch (e: Exception) {
            MerchantRegistrationResult(
                success = false,
                errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    override suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration,
        activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        // For mock implementation, we ignore the ActivityResultSender and use the same logic
        return registerMerchantAtomic(merchantRegistration)
    }

    override suspend fun getMerchantStatus(walletAddress: String): Flow<MerchantStatus> = flow {
        delay(500) // Simulate network delay

        val status = registeredMerchants[walletAddress] ?: MerchantStatus(
            isRegistered = false
        )

        emit(status)
    }

    override suspend fun getMerchantAccountData(merchantAccount: String): Flow<MerchantStatus> = flow {
        delay(500) // Simulate network delay

        // Find merchant by account
        val status = registeredMerchants.values.find { it.merchantAccount == merchantAccount }
            ?: MerchantStatus(isRegistered = false)

        emit(status)
    }

    private fun generateMockTransactionSignature(): String {
        return "mock_tx_" + (1..64).map { "0123456789abcdef".random() }.joinToString("")
    }

    private fun generateMockMerchantAccount(merchantPublicKey: String): String {
        return "merchant_" + merchantPublicKey.take(8) + "_" + System.currentTimeMillis().toString().takeLast(6)
    }

    private fun getCurrentTimestamp(): String {
        return System.currentTimeMillis().toString()
    }
}
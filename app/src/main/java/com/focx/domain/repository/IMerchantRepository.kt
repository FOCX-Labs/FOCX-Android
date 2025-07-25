package com.focx.domain.repository

import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.flow.Flow

interface IMerchantRepository {
    /**
     * Register merchant using Solana program register_merchant_atomic instruction
     * @param registration Merchant registration data
     * @return Flow of registration result
     */
    suspend fun registerMerchantAtomic(merchantRegistration: MerchantRegistration): MerchantRegistrationResult

    /**
     * Register merchant using Solana program register_merchant_atomic instruction with wallet interaction
     * @param registration Merchant registration data
     * @param activityResultSender Activity result sender for wallet interaction
     * @return Flow of registration result
     */
    suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration,
        activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult

    /**
     * Get merchant account data from Solana program
     * @param merchantAccount Merchant account address
     * @return Flow of merchant status
     */
    suspend fun getMerchantAccountData(walletAddress: String): Flow<MerchantStatus>

    /**
     * Deposit merchant funds (security deposit) using Solana program deposit_merchant_funds instruction
     * @param merchantAccount Merchant account address
     * @param depositAmount Amount to deposit in USDC (raw amount with decimals)
     * @param activityResultSender Activity result sender for wallet interaction
     * @return Result of deposit operation
     */
    suspend fun depositMerchantFunds(
        merchantAccount: String,
        depositAmount: Long,
        activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult
}
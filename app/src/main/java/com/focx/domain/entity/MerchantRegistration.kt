package com.focx.domain.entity

import com.focx.core.constants.AppConstants
import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

data class MerchantRegistration(
    val name: String,
    val description: String,
    val merchantPublicKey: String,
    val payerPublicKey: String,
    val securityDeposit: ULong = AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT,
    val programId: String = AppConstants.App.PROGRAM_ID
)

data class MerchantRegistrationResult(
    val success: Boolean,
    val transactionSignature: String? = null,
    val errorMessage: String? = null,
    val merchantAccount: String? = null
)

data class MerchantStatus(
    val isRegistered: Boolean,
    val merchantAccount: String? = null,
    val registrationDate: String? = null,
    val securityDeposit: ULong? = null,
    val status: String = AppConstants.Merchant.DEFAULT_STATUS
)

@Serializable
data class Merchant(
    val discriminator: Long,
    val owner: SolanaPublicKey,
    val name: String,
    val description: String,
    val productCount: ULong,
    val totalSales: ULong,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val depositAmount: ULong,
    val depositTokenMint: SolanaPublicKey,
    val depositLocked: ULong,
    val depositUpdatedAt: Long,
    val bump: UByte
)
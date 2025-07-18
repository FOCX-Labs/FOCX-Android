package com.focx.domain.entity

import com.focx.core.constants.AppConstants

data class MerchantRegistration(
    val name: String,
    val description: String,
    val merchantPublicKey: String,
    val payerPublicKey: String,
    val securityDeposit: String = AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT,
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
    val securityDeposit: String? = null,
    val status: String = AppConstants.Merchant.DEFAULT_STATUS
)
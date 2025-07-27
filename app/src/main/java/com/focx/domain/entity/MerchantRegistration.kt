package com.focx.domain.entity

import com.focx.core.constants.AppConstants

data class MerchantRegistration(
    val name: String,
    val description: String,
    val merchantPublicKey: String,
    val payerPublicKey: String,
    val securityDeposit: ULong = AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT,
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
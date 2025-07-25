package com.focx.domain.entity

import com.focx.core.constants.AppConstants
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

data class MerchantRegistration(
    val name: String,
    val description: String,
    val merchantPublicKey: String,
    val payerPublicKey: String,
    val securityDeposit: Long = AppConstants.Wallet.DEFAULT_SECURITY_DEPOSIT,
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
    val securityDeposit: Long? = null,
    val status: String = AppConstants.Merchant.DEFAULT_STATUS
)

@Serializable
data class Merchant(
    val owner: SolanaPublicKey,       // 商户所有者
    val name: String,                 // 商户名称
    val description: String,          // 商户描述
    val productCount: Long,           // 商品数量
    val totalSales: Long,             // 总销量
    val isActive: Boolean,            // 是否活跃
    val createdAt: Long,              // 创建时间
    val updatedAt: Long,              // 更新时间
    val depositAmount: Long,          // 保证金数量
    val depositTokenMint: SolanaPublicKey,     // 保证金代币地址（Pubkey 用 String 表示）
    val depositLocked: Long,          // 锁定的保证金
    val depositUpdatedAt: Long,       // 保证金更新时间
    val bump: UByte                   // PDA bump
)
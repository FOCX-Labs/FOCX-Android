package com.focx.domain.entity

import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class GlobalIdRoot(
    val discriminator: Long,
    val lastMerchantId: UInt,
    val lastGlobalId: ULong,
    val chunkSize: UInt,
    val merchants: List<SolanaPublicKey>,
    val maxProductsPerShard: UShort,
    val maxKeywordsPerProduct: UByte,
    val bloomFilterSize: UShort,
    val bump: UByte
)

@Serializable
data class IdChunk(
    val discriminator: Long,
    val merchantId: UInt,
    val chunkIndex: UInt,
    val startId: ULong,
    val endId: ULong,
    val nextAvailable: ULong,
    val bitmap: ByteArray,
    val bump: UByte
)

@Serializable
data class KeywordRoot(
    val discriminator: Long,
    val keyword: String,
    val totalShards: UByte,
    val firstShard: SolanaPublicKey,
    val lastShard: SolanaPublicKey,
    val totalProducts: UInt,
    val bloomFilter: ByteArray, // [u8; 256]
    val bump: UByte
)

@Serializable
data class KeywordShard(
    val discriminator: Long,
    val keyword: String,
    val shardIndex: UInt,
    val prevShard: SolanaPublicKey,
    val nextShard: SolanaPublicKey?,
    val productIds: List<ULong>,
    val minId: ULong,
    val maxId: ULong,
    val bloomSummary: ByteArray, // [u8; 32]
    val bump: UByte
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

@Serializable
data class MerchantDepositInfo(
    val discriminator: Long,
    val totalDeposit: ULong,
    val lockedDeposit: ULong,
    val availableDeposit: ULong,
    val requiredDeposit: ULong,
    val isSufficient: Boolean,
    val depositTokenMint: SolanaPublicKey,
    val lastUpdated: Long
)

@Serializable
data class MerchantIdAccount(
    val discriminator: Long,
    val merchantId: UInt,
    val lastChunkIndex: UInt,
    val lastLocalId: ULong,
    val activeChunk: SolanaPublicKey,
    val unusedChunks: List<SolanaPublicKey>,
    val bump: UByte
)

@Serializable
data class MerchantRegisteredAtomic(
    val discriminator: Long,
    val merchant: SolanaPublicKey,
    val merchantId: UInt,
    val name: String,
    val initialIdRangeStart: ULong,
    val initialIdRangeEnd: ULong
)

@Serializable
data class MerchantStats(
    val discriminator: Long,
    val productCount: ULong,
    val totalSales: ULong,
    val activeProducts: ULong,
    val totalKeywords: ULong,
    val avgProductPrice: ULong,
    val lastUpdated: Long
)

@Serializable
enum class OrderManagementStatus {
    Pending,
    Shipped,
    Delivered,
    Refunded
}

@Serializable
data class SolanaOrder(
    val discriminator: Long,
    val buyer: SolanaPublicKey,
    val merchant: SolanaPublicKey,
    val productId: ULong,
    val quantity: UInt,
    val price: ULong,
    val totalAmount: ULong,
    val paymentToken: SolanaPublicKey,
    val status: OrderManagementStatus,
    val shippingAddress: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val confirmedAt: Long?,
    val shippedAt: Long?,
    val deliveredAt: Long?,
    val refundedAt: Long?,
    val refundRequestedAt: Long?,
    val refundReason: String,
    val trackingNumber: String,
    val transactionSignature: String,
    val bump: UByte
)

@Serializable
data class OrderStats(
    val discriminator: Long,
    val totalOrders: ULong,
    val pendingOrders: ULong,
    val shippedOrders: ULong,
    val deliveredOrders: ULong,
    val refundedOrders: ULong,
    val totalRevenue: ULong,
    val bump: UByte
)

@Serializable
data class SupportedToken(
    val mint: SolanaPublicKey,
    val symbol: String,
    val isActive: Boolean
)

@Serializable
data class PaymentConfig(
    val discriminator: Long,
    val authority: SolanaPublicKey,
    val supportedTokens: List<SupportedToken>,
    val feeRate: UShort,
    val feeRecipient: SolanaPublicKey,
    val createdAt: Long,
    val updatedAt: Long,
    val bump: UByte
)

@Serializable
data class PriceIndexNode(
    val discriminator: Long,
    val priceRangeStart: ULong,
    val priceRangeEnd: ULong,
    val productIds: List<ULong>,
    val leftChild: SolanaPublicKey?,
    val rightChild: SolanaPublicKey?,
    val parent: SolanaPublicKey?,
    val height: UByte,
    val bump: UByte
)

@Serializable
data class ProductBase(
    val discriminator: Long,
    val id: ULong,
    val merchant: SolanaPublicKey,
    val name: String,
    val description: String,
    val price: ULong,
    val keywords: String,
    val inventory: ULong,
    val sales: UInt,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val paymentToken: SolanaPublicKey,
    val shippingLocation: String,
    val bump: UByte
)

@Serializable
data class ProductEvent(
    val discriminator: Long,
    val productId: ULong,
    val merchant: SolanaPublicKey,
    val name: String,
    val description: String,
    val price: ULong,
    val keywords: List<String>,
    val salesCount: UInt,
    val isActive: Boolean,
    val timestamp: Long,
    val eventType: String
)

@Serializable
data class ProductExtended(
    val discriminator: Long,
    val productId: ULong,
    val imageVideoUrls: String,
    val salesRegions: String,
    val logisticsMethods: String,
    val bump: UByte
)

@Serializable
data class ProductSales(
    val discriminator: Long,
    val productId: ULong,
    val merchant: SolanaPublicKey,
    val name: String,
    val price: ULong,
    val sales: UInt,
    val lastUpdate: Long
)

@Serializable
data class SalesIndexNode(
    val discriminator: Long,
    val salesRangeStart: UInt,
    val salesRangeEnd: UInt,
    val productIds: List<ULong>,
    val topItems: List<ProductSales>,
    val leftChild: SolanaPublicKey?,
    val rightChild: SolanaPublicKey?,
    val parent: SolanaPublicKey?,
    val height: UByte,
    val bump: UByte
)

@Serializable
data class SystemConfig(
    val discriminator: Long,
    val authority: SolanaPublicKey,
    val maxProductsPerShard: UShort,
    val maxKeywordsPerProduct: UByte,
    val chunkSize: UInt,
    val bloomFilterSize: UShort,
    val merchantDepositRequired: ULong,
    val depositTokenMint: SolanaPublicKey,
    val platformFeeRate: UShort,
    val platformFeeRecipient: SolanaPublicKey,
    val autoConfirmDays: UInt,
    val vaultProgramId: SolanaPublicKey,
    val vaultTokenAccount: SolanaPublicKey,
    val platformTokenAccount: SolanaPublicKey,

)

@Serializable
data class UserPurchaseCount(
    val discriminator: Long,
    val buyer: SolanaPublicKey,
    val purchaseCount: ULong,
    val createdAt: Long,
    val updatedAt: Long,
    val bump: UByte
)

@Serializable
data class MerchantOrderCount(
    val discriminator: Long,
    val merchant: SolanaPublicKey,
    val totalOrders: ULong,
    val createAt: Long,
    val updateAt: Long,
    val bump: UByte
)

@Serializable
data class MerchantOrder(
    val discriminator: Long,
    val merchant: SolanaPublicKey,
    val buyer: SolanaPublicKey,
    val merchantOrderSequence: ULong,
    val buyerOrderPda: SolanaPublicKey,
    val productId: ULong,
    val createAt: Long,
    val bump: UByte
)
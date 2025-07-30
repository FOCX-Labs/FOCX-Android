package com.focx.utils

import android.annotation.SuppressLint
import com.focx.core.constants.AppConstants
import com.focx.domain.entity.MerchantOrder
import com.focx.domain.entity.MerchantOrderCount
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderItem
import com.focx.domain.entity.Product
import com.focx.domain.entity.ProductBase
import com.focx.domain.entity.ProductExtended
import com.focx.domain.entity.ShippingAddress
import com.focx.domain.entity.SolanaOrder
import com.focx.domain.entity.SortOrder
import com.focx.domain.entity.SystemConfig
import com.focx.domain.entity.UserPurchaseCount
import com.funkatronics.kborsh.Borsh
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlinx.serialization.encodeToByteArray
import kotlin.math.max
import kotlin.math.min

object ShopUtils {
    private const val TAG = "ShopUtils"
    suspend fun getMerchantInfoPda(
        merchantPublicKey: PublicKey
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("merchant_info".toByteArray(), merchantPublicKey.bytes),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getSystemConfigPDA(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("system_config".toByteArray()),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getMerchantIdPda(
        merchantPublicKey: PublicKey
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("merchant_id".toByteArray(), merchantPublicKey.bytes),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getInitialChunkPda(
        merchantPublicKey: PublicKey,
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "id_chunk".toByteArray(),
                merchantPublicKey.bytes,
                byteArrayOf(0),
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getGlobalRootPda(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("global_id_root".toByteArray()),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getDepositEscrowPda(
        mint: SolanaPublicKey = AppConstants.App.getMint(),
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "deposit_escrow".toByteArray(),
                mint.bytes
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getAssociatedTokenAddress(
        owner: SolanaPublicKey,
        mint: SolanaPublicKey = AppConstants.App.getMint(),
        tokenProgramId: SolanaPublicKey = SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID)
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                owner.bytes,
                tokenProgramId.bytes,
                mint.bytes
            ),
            SolanaPublicKey.from(AppConstants.App.ASSOCIATED_TOKEN_PROGRAM_ID)
        )
    }

    suspend fun getPaymentPda(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("payment_config".toByteArray()),
            AppConstants.App.getShopProgramId()
        )
    }

    fun genTransactionInstruction(
        accounts: List<AccountMeta>,
        data: ByteArray,
        programId: SolanaPublicKey = AppConstants.App.getShopProgramId()
    ): TransactionInstruction {
        Log.d(TAG, "============genTransactionInstruction : $programId")
        accounts.forEachIndexed { index, meta ->
            Log.d(
                TAG,
                "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}"
            )
        }
        Log.d(TAG, "  instructionData: ${data.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${data.joinToString("") { "%02x".format(it) }}")
        return TransactionInstruction(programId, accounts, data)
    }

    suspend fun getProductBasePDA(productId: ULong): Result<ProgramDerivedAddress> {
        Log.d(TAG, "  getProductBasePDA productId: $productId")

        return ProgramDerivedAddress.find(
            listOf(
                "product".toByteArray(),
                Borsh.encodeToByteArray(productId)
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getProductExtendedPDA(productId: ULong): Result<ProgramDerivedAddress> {
        Log.d(TAG, "  getProductExtendedPDA productId: $productId")

        return ProgramDerivedAddress.find(
            listOf(
                "product_extended".toByteArray(),
                Borsh.encodeToByteArray(productId)
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getKeywordRootPda(keyword: String): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "keyword_root".toByteArray(),
                keyword.toByteArray()
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getTargetShardPda(keyword: String): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "keyword_shard".toByteArray(),
                keyword.toByteArray(),
                Borsh.encodeToByteArray(0u),
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    fun calcPriceRange(price: ULong): Pair<ULong, ULong> {
        if (price == 0UL) return 0UL to 1UL

        val start = 1UL shl (63 - price.countLeadingZeroBits())
        val end = if (start > price) start else start shl 1

        return start to end
    }

    suspend fun getPriceIndexPDA(priceRange: Pair<ULong, ULong>): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "price_index".toByteArray(),
                Borsh.encodeToByteArray(priceRange.first),
                Borsh.encodeToByteArray(priceRange.second),
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    fun calcSalesRange(sales: UInt): Pair<UInt, UInt> {
        if (sales == 0u) return 0u to 10u

        var power = 1u
        while (power * 10u <= sales && power * 10u > power) {
            power *= 10u
        }

        val start = power
        val end = if (sales > start) start * 10u else start * 10u

        return start to end
    }


    suspend fun getSalesIndexPDA(range: Pair<UInt, UInt>): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "sales_index".toByteArray(),
                Borsh.encodeToByteArray(range.first),
                Borsh.encodeToByteArray(range.second),
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    @SuppressLint("DefaultLocale")
    fun getPriceShow(price: ULong): String {
        return String.format(
            String.format(
                "%.2f",
                (price.toDouble() / AppConstants.App.TOKEN_DECIMAL)
            )
        )
    }

    suspend fun getUserPurchaseCountPDA(buyer: SolanaPublicKey): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "user_purchase_count".toByteArray(),
                buyer.bytes
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getOrderPda(
        buyerPubkey: SolanaPublicKey,
        currentPurchaseCount: ULong
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "buyer_order".toByteArray(),
                buyerPubkey.bytes,
                Borsh.encodeToByteArray(currentPurchaseCount)
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getOrderStatsPda(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "order_stats".toByteArray()
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getProgramTokenAccountPda(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "program_token_account".toByteArray(),
                AppConstants.App.getMint().bytes
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getSimplePda(key: String): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                key.toByteArray()
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getProductInfoById(productId: ULong, solanaRpcClient: SolanaRpcClient): Product? {
        val productPda = getProductBasePDA(productId).getOrNull()!!
        val baseInfo = solanaRpcClient.getAccountInfo<ProductBase>(productPda).result?.data

        if (baseInfo == null) {
            return null
        }

        val productExtendedPda = getProductExtendedPDA(productId).getOrNull()!!
        val extendedInfo =
            solanaRpcClient.getAccountInfo<ProductExtended>(productExtendedPda).result?.data

        val product = Product(
            baseInfo.id, //            val id: ULong,
            baseInfo.name, //        val name: String,
            baseInfo.description, //        val description: String,
            baseInfo.price, //        val price: ULong,
            "USDC", //        val currency: String = "USDC",
            if (extendedInfo != null && extendedInfo.imageVideoUrls.isNotEmpty()) extendedInfo.imageVideoUrls.split(
                ","
            ) else emptyList(), //        val imageUrls: List<String>,
            baseInfo.merchant.base58(), //        val sellerId: String,
            "", //        val sellerName: String,
            "", //        val category: String,
            baseInfo.inventory.toInt(), //        val stock: Int,
            baseInfo.sales.toInt(), //        val salesCount: Int = 0,
            "", //        val shippingFrom: String,
            emptyList(), //        val shippingMethods: List<String>,
            emptyList() //        val specifications: Map<String, String> = emptyMap(),
//        val rating: Float = 0f,
//        val reviewCount: Int = 0,
        )

        return product
    }

    suspend fun getOrderInfoByPda(
        orderPda: SolanaPublicKey,
        solanaRpcClient: SolanaRpcClient
    ): Order {
        val orderInfo = solanaRpcClient.getAccountInfo<SolanaOrder>(orderPda).result?.data!!
        val productInfo = getProductInfoById(orderInfo.productId, solanaRpcClient)!!
        return Order(
            id = orderPda.base58(),
            buyerId = orderInfo.buyer.base58(),
            sellerId = orderInfo.merchant.base58(),
            sellerName = "",
            items = listOf(
                OrderItem(
                    id = productInfo.id.toString(),
                    productId = productInfo.id.toString(),
                    productName = productInfo.name,
                    productImage = productInfo.imageUrls[0],
                    quantity = orderInfo.quantity.toInt(),
                    unitPrice = productInfo.price.toDouble() / AppConstants.App.TOKEN_DECIMAL,
                    totalPrice = productInfo.price.toDouble()
                        .times(orderInfo.quantity.toInt()) / AppConstants.App.TOKEN_DECIMAL
                )
            ),
            totalAmount = orderInfo.totalAmount.toDouble() / AppConstants.App.TOKEN_DECIMAL,
            currency = "USDC",
            status = orderInfo.status,
            shippingAddress = ShippingAddress(
                recipientName = "",
                addressLine1 = orderInfo.shippingAddress,
                addressLine2 = "",
                city = "",
                state = "",
                postalCode = "",
                country = "",
                phoneNumber = ""
            ),
            paymentMethod = "USDC",
            transactionHash = orderInfo.transactionSignature,
            trackingNumber = orderInfo.trackingNumber,
            orderDate = orderInfo.createdAt,
            updatedAt = orderInfo.updatedAt,
            estimatedDelivery = orderInfo.deliveredAt
        )
    }

    suspend fun getMerchantOrderPDA(
        merchantPubkey: SolanaPublicKey,
        merchantOrderCount: ULong
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "merchant_order".toByteArray(),
                merchantPubkey.bytes,
                Borsh.encodeToByteArray(merchantOrderCount)
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getMerchantOrderCountPDA(
        merchantPubkey: SolanaPublicKey,
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "merchant_order_count".toByteArray(),
                merchantPubkey.bytes,
            ),
            AppConstants.App.getShopProgramId()
        )
    }

    suspend fun getMerchantOrderCount(
        merchantOrderCountPda: SolanaPublicKey,
        solanaRpcClient: SolanaRpcClient
    ): ULong {
        val data =
            solanaRpcClient.getAccountInfo<MerchantOrderCount>(merchantOrderCountPda).result?.data
        return data?.totalOrders ?: 0UL
    }

    suspend fun getMerchantOrder(
        merchantOrderPda: SolanaPublicKey,
        solanaRpcClient: SolanaRpcClient
    ): MerchantOrder? {
        return solanaRpcClient.getAccountInfo<MerchantOrder>(merchantOrderPda).result?.data
    }

    fun calcPageInfo(
        page: Int,
        pageSize: Int,
        total: Int,
        sortOrder: SortOrder = SortOrder.DESC
    ): Pair<Int, Int> {
        val startIndex = if (sortOrder == SortOrder.DESC) max(
            1,
            total - page * pageSize + 1
        ) else min(1 + pageSize * (page - 1), total)
        val endIndex = min(total, startIndex + pageSize)

        return Pair(startIndex, endIndex)
    }

    suspend fun getOrdersBySeller(
        sellerId: String,
        solanaRpcClient: SolanaRpcClient,
        page: Int = 1,
        pageSize: Int = 10,
        sortOrder: SortOrder = SortOrder.DESC
    ): List<Order> {
        val merchantPubKey = SolanaPublicKey.from(sellerId)
        val pda = getMerchantOrderCountPDA(merchantPubKey).getOrNull()!!
        val orderCount = getMerchantOrderCount(pda, solanaRpcClient)

        val pageInfo = calcPageInfo(page, pageSize, orderCount.toInt(), sortOrder)
        val orderList = ArrayList<Order>()
        if (orderCount.toInt() < pageInfo.first){
            return emptyList()
        }

        for (i in pageInfo.first..pageInfo.second) {
            try {
                val merchantOrderPda =
                    getMerchantOrderPDA(merchantPubKey, i.toULong()).getOrNull()!!
                val orderPda =
                    getMerchantOrder(merchantOrderPda, solanaRpcClient)?.buyerOrderPda!!
                val order = getOrderInfoByPda(orderPda, solanaRpcClient)
                orderList.add(order)
            } catch (e: Exception) {
                Log.e(TAG, "getOrdersBySeller", e)
            }
        }
        return orderList
    }

    suspend fun getUserPurchaseCount(
        buyerPubkey: SolanaPublicKey,
        solanaRpcClient: SolanaRpcClient
    ): UserPurchaseCount {
        val userPurchaseCountPda = getUserPurchaseCountPDA(buyerPubkey).getOrNull()!!
        return solanaRpcClient.getAccountInfo<UserPurchaseCount>(userPurchaseCountPda).result?.data!!
    }

    suspend fun getOrdersByBuyer(
        buyerId: String,
        solanaRpcClient: SolanaRpcClient,
        page: Int = 1,
        pageSize: Int = 10,
        sortOrder: SortOrder = SortOrder.DESC
    ): List<Order> {
        val buyerPubkey = SolanaPublicKey.from(buyerId)
        val orderCount = getUserPurchaseCount(buyerPubkey, solanaRpcClient).purchaseCount

        val pageInfo = calcPageInfo(page, pageSize, orderCount.toInt(), sortOrder)
        if (orderCount.toInt() < pageInfo.first){
            return emptyList()
        }
        val orderList = ArrayList<Order>()
        for (i in pageInfo.first..pageInfo.second) {
            val orderPda = getOrderPda(buyerPubkey, i.toULong()).getOrNull()!!
            val order = getOrderInfoByPda(orderPda, solanaRpcClient)
            orderList.add(order)
        }
        return orderList
    }

    suspend fun getSystemConfig(solanaRpcClient: SolanaRpcClient): SystemConfig {
        val systemConfigPda = getSystemConfigPDA().getOrNull()!!
        return solanaRpcClient.getAccountInfo<SystemConfig>(systemConfigPda).result?.data!!
    }
}
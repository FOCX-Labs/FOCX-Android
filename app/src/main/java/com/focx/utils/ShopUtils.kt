package com.focx.utils

import com.focx.core.constants.AppConstants
import com.funkatronics.kborsh.Borsh
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlinx.serialization.encodeToByteArray

object ShopUtils {
    private val TAG = "ShopUtils"
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

    suspend fun getDepositEscrowPda(): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("deposit_escrow".toByteArray()),
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


}
package com.focx.utils

import com.focx.core.constants.AppConstants
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey

object ShopUtils {
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
}
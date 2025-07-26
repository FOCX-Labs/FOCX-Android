package com.focx.utils

import com.focx.core.constants.AppConstants
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey

object ShopUtils {
    suspend fun getMerchantInfoPda(
        merchantPublicKey: PublicKey,
        programId: PublicKey
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("merchant_info".toByteArray(), merchantPublicKey.bytes), programId
        )
    }

    suspend fun getSystemConfigPDA(programId: PublicKey): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("system_config".toByteArray()), programId
        )
    }

    suspend fun getMerchantIdPda(
        merchantPublicKey: PublicKey,
        programId: PublicKey
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("merchant_id".toByteArray(), merchantPublicKey.bytes), programId
        )
    }

    suspend fun getInitialChunkPda(
        merchantPublicKey: PublicKey,
        programId: PublicKey
    ): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf(
                "id_chunk".toByteArray(),
                merchantPublicKey.bytes,
                byteArrayOf(0),
            ), programId
        )
    }

    suspend fun getGlobalRootPda(programId: PublicKey): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("global_id_root".toByteArray()), programId
        )
    }

    suspend fun getDepositEscrowPda(programId: PublicKey): Result<ProgramDerivedAddress> {
        return ProgramDerivedAddress.find(
            listOf("deposit_escrow".toByteArray()), programId
        )
    }

    suspend fun getAssociatedTokenAddress(
        mint: SolanaPublicKey,
        owner: SolanaPublicKey,
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
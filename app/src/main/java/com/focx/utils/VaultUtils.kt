package com.focx.utils

import com.focx.core.constants.AppConstants
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey

object VaultUtils {
    const val VAULT_NAME = "Insurance Fund Vault"

    suspend fun getVaultPda(): SolanaPublicKey {
        val vaultNameBuffer = ByteArray(32)
        val vaultNameBytes = VAULT_NAME.toByteArray()

        System.arraycopy(vaultNameBytes, 0, vaultNameBuffer, 0, minOf(vaultNameBytes.size, 32))

        return ProgramDerivedAddress.find(
            listOf(
                "vault".toByteArray(),
                vaultNameBuffer
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }

    suspend fun getVaultDepositorPda(account: SolanaPublicKey): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "vault_depositor".toByteArray(),
                getVaultPda().bytes,
                account.bytes
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }

    suspend fun getVaultTokenAccountPda(): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "vault_token_account".toByteArray(),
                getVaultPda().bytes
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }
}
package com.focx.core.constants

import com.focx.core.network.NetworkConfig
import com.focx.core.network.NetworkConfig.MAINNET
import com.solana.publickey.SolanaPublicKey

/**
 * Application-wide constants
 */
object AppConstants {

    object App {
        //SPL Token Program
        const val SPL_TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

        //Associated Token Program
        const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

        const val MAINNET_SHOP_PROGRAM_ID = "5XZ74thixMBX2tQN9P3yLTugUK4YMdRLznDNa2mRdGNT"
        const val DEVNET_SHOP_PROGRAM_ID = "5XZ74thixMBX2tQN9P3yLTugUK4YMdRLznDNa2mRdGNT"

        const val TOKEN_DECIMAL = 1_000_000_000L

        fun getShopProgramId(): SolanaPublicKey {
            val programId =  when (NetworkConfig.getCurrentNetwork()) {
                MAINNET -> MAINNET_SHOP_PROGRAM_ID
                else -> DEVNET_SHOP_PROGRAM_ID
            }

            return SolanaPublicKey.from(programId)
        }

        const val MAINNET_VAULT_PROGRAM_ID = "EHiKn3J5wywNG2rHV2Qt74AfNqtJajhPerkVzYXudEwn"
        const val DEVNET_VAULT_PROGRAM_ID = "EHiKn3J5wywNG2rHV2Qt74AfNqtJajhPerkVzYXudEwn"

        fun getVaultProgramId(): SolanaPublicKey {
            val programId =  when (NetworkConfig.getCurrentNetwork()) {
                MAINNET -> MAINNET_VAULT_PROGRAM_ID
                else -> DEVNET_VAULT_PROGRAM_ID
            }

            return SolanaPublicKey.from(programId)
        }

        const val MAINNET_USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        const val DEVNET_USDC_FOCX_MINT = "DXDVt289yXEcqXDd9Ub3HqSBTWwrmNB8DzQEagv9Svtu"
        fun getMint(): SolanaPublicKey {
            val mint = when (NetworkConfig.getCurrentNetwork()) {
                MAINNET -> MAINNET_USDC_MINT
                else -> DEVNET_USDC_FOCX_MINT
            }

            return SolanaPublicKey.from(mint)
        }


    }

    /**
     * Wallet and Blockchain Configuration
     */
    object Wallet {
        const val DEFAULT_SECURITY_DEPOSIT = 1_000_000_000_000UL // 1000 USDC amount
    }


    /**
     * Merchant Configuration
     */
    object Merchant {
        const val DEFAULT_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
        const val SUSPENDED_STATUS = "SUSPENDED"
    }
}
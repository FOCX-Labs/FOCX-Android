package com.focx.core.constants

import com.focx.core.network.NetworkConfig
import com.focx.core.network.NetworkConfig.MAINNET
import com.solana.publickey.SolanaPublicKey

/**
 * Application-wide constants
 */
object AppConstants {

    object App {
        const val PROGRAM_ID = "5XZ74thixMBX2tQN9P3yLTugUK4YMdRLznDNa2mRdGNT"

        //SPL Token Program
        const val SPL_TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

        //Associated Token Program
        const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

        //mainKeypair, Galahad wallet address
        const val AUTHORITY_PUBLIC_KEY = "AyrDj67STAAt9UHc28S694scrq955vbH54rAMtmuQBME"
        const val APP_NAME = "FOCX"
        const val APP_VERSION = "1.0.0"
        const val APP_IDENTITY_URI = "https://focx.app"
        const val APP_IDENTITY_ICON = "favicon.ico"

        const val MAINNET_PROGRAM_ID = "5XZ74thixMBX2tQN9P3yLTugUK4YMdRLznDNa2mRdGNT"
        const val DEVNET_PROGRAM_ID = "5XZ74thixMBX2tQN9P3yLTugUK4YMdRLznDNa2mRdGNT"

        fun getProgramId(): SolanaPublicKey {
            val programId =  when (NetworkConfig.getCurrentNetwork()) {
                MAINNET -> MAINNET_PROGRAM_ID
                else -> DEVNET_PROGRAM_ID
            }

            return SolanaPublicKey.from(programId)
        }

        const val MAINNET_MINT = "DXDVt289yXEcqXDd9Ub3HqSBTWwrmNB8DzQEagv9Svtu"
        const val DEVNET_MINT = "DXDVt289yXEcqXDd9Ub3HqSBTWwrmNB8DzQEagv9Svtu"
        fun getMint(): SolanaPublicKey {
            val mint = when (NetworkConfig.getCurrentNetwork()) {
                MAINNET -> MAINNET_MINT
                else -> DEVNET_MINT
            }

            return SolanaPublicKey.from(mint)
        }


    }

    /**
     * Wallet and Blockchain Configuration
     */
    object Wallet {
        const val DEFAULT_SECURITY_DEPOSIT = 1000UL // USDC amount
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
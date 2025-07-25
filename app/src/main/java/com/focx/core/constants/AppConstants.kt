package com.focx.core.constants

/**
 * Application-wide constants
 */
object AppConstants {

    object App {
        const val PROGRAM_ID = "mo5xPstZDm27CAkcyoTJnEovMYcW45tViAU6PZikv5q"

        //SPL Token Program
        const val SPL_TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

        //Associated Token Program
        const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

        //USDC Token Mint (Devnet) //TODO Check Address
        const val USDC_MINT = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"

        const val USDC_FOCX_MINT = "DXDVt289yXEcqXDd9Ub3HqSBTWwrmNB8DzQEagv9Svtu"

        //mainKeypair, Galahad wallet address
        const val AUTHORITY_PUBLIC_KEY = "AyrDj67STAAt9UHc28S694scrq955vbH54rAMtmuQBME"
        const val APP_NAME = "FOCX"
        const val APP_VERSION = "1.0.0"
        const val APP_IDENTITY_URI = "https://focx.app"
        const val APP_IDENTITY_ICON = "favicon.ico"
    }

    /**
     * Wallet and Blockchain Configuration
     */
    object Wallet {
        const val DEFAULT_SECURITY_DEPOSIT = 1000L // USDC amount
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
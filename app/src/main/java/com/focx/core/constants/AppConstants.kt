package com.focx.core.constants

/**
 * Application-wide constants
 */
object AppConstants {

    object App {
        const val PROGRAM_ID = "H2ijJPLXRpj2Vw9mSPUSDU7tFZfqVSWkA5xZEkxdfin7"
        const val APP_NAME = "FOCX"
        const val APP_VERSION = "1.0.0"
        const val APP_IDENTITY_URI = "https://focx.app"
        const val APP_IDENTITY_ICON = "favicon.ico"
    }

    /**
     * Wallet and Blockchain Configuration
     */
    object Wallet {
        const val DEFAULT_SECURITY_DEPOSIT = "1000" // USDC amount
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
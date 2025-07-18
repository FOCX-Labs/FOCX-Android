package com.focx.data.constants

/**
 * SharedPreferences Configuration Constants
 * Centralized management of all SharedPreferences related configurations
 */
object PreferencesConstants {

    /**
     * SharedPreferences file names
     */
    const val WALLET_PREFS_NAME = "focx_wallet_prefs"
    const val USER_PREFS_NAME = "focx_user_prefs"
    const val APP_PREFS_NAME = "focx_app_prefs"

    /**
     * Wallet related preference keys
     */
    object WalletKeys {
        const val WALLET_PUBLIC_KEY = "wallet_public_key"
        const val WALLET_ACCOUNT_LABEL = "wallet_account_label"
        const val WALLET_AUTH_TOKEN = "wallet_auth_token"
        const val WALLET_CONNECTED = "wallet_connected"
        const val WALLET_BALANCE = "wallet_balance"
    }

    /**
     * User profile related preference keys
     */
    object UserKeys {
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val USER_AVATAR = "user_avatar"
        const val IS_SELLER_REGISTERED = "is_seller_registered"
        const val SELLER_ID = "seller_id"
    }

    /**
     * Application settings preference keys
     */
    object AppKeys {
        const val THEME_MODE = "theme_mode"
        const val LANGUAGE = "language"
        const val NOTIFICATION_ENABLED = "notification_enabled"
        const val FIRST_LAUNCH = "first_launch"
        const val APP_VERSION = "app_version"
    }

    /**
     * Security related preference keys
     */
    object SecurityKeys {
        const val BIOMETRIC_ENABLED = "biometric_enabled"
        const val PIN_ENABLED = "pin_enabled"
        const val AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
    }
}
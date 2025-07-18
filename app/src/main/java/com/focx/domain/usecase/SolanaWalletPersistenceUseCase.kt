package com.focx.domain.usecase

import android.content.SharedPreferences
import com.focx.data.constants.PreferencesConstants
import com.focx.domain.entity.Connected
import com.focx.domain.entity.NotConnected
import com.focx.domain.entity.WalletConnection
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaWalletPersistenceUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    private var connection: WalletConnection = NotConnected

    fun getWalletConnection(): WalletConnection {
        return when (connection) {
            is Connected -> connection
            is NotConnected -> {
                val key = sharedPreferences.getString(PreferencesConstants.WalletKeys.WALLET_PUBLIC_KEY, "")
                val accountLabel =
                    sharedPreferences.getString(PreferencesConstants.WalletKeys.WALLET_ACCOUNT_LABEL, "") ?: ""
                val token = sharedPreferences.getString(PreferencesConstants.WalletKeys.WALLET_AUTH_TOKEN, "")

                val newConn = if (key.isNullOrEmpty() || token.isNullOrEmpty()) {
                    NotConnected
                } else {
                    Connected(SolanaPublicKey.from(key), accountLabel, token)
                }

                connection = newConn
                return newConn
            }
        }
    }

    fun persistConnection(pubKey: SolanaPublicKey, accountLabel: String, token: String) {
        sharedPreferences.edit().apply {
            putString(PreferencesConstants.WalletKeys.WALLET_PUBLIC_KEY, pubKey.base58())
            putString(PreferencesConstants.WalletKeys.WALLET_ACCOUNT_LABEL, accountLabel)
            putString(PreferencesConstants.WalletKeys.WALLET_AUTH_TOKEN, token)
        }.apply()

        connection = Connected(pubKey, accountLabel, token)
    }

    fun clearConnection() {
        sharedPreferences.edit().apply {
            putString(PreferencesConstants.WalletKeys.WALLET_PUBLIC_KEY, "")
            putString(PreferencesConstants.WalletKeys.WALLET_ACCOUNT_LABEL, "")
            putString(PreferencesConstants.WalletKeys.WALLET_AUTH_TOKEN, "")
        }.apply()

        connection = NotConnected
    }


}
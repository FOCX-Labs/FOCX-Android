package com.focx.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "account_cache",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_MERCHANT_REGISTERED = "merchant_registered_"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp_"
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun isMerchantRegistered(walletAddress: String): Boolean? {
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP + walletAddress, 0L)
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is still valid
        if (currentTime - timestamp > CACHE_DURATION) {
            // Cache expired, remove it
            clearMerchantCache(walletAddress)
            return null
        }
        
        return if (prefs.contains(KEY_MERCHANT_REGISTERED + walletAddress)) {
            prefs.getBoolean(KEY_MERCHANT_REGISTERED + walletAddress, false)
        } else {
            null
        }
    }

    fun setMerchantRegistered(walletAddress: String, isRegistered: Boolean) {
        prefs.edit()
            .putBoolean(KEY_MERCHANT_REGISTERED + walletAddress, isRegistered)
            .putLong(KEY_CACHE_TIMESTAMP + walletAddress, System.currentTimeMillis())
            .apply()
    }

    fun clearMerchantCache(walletAddress: String) {
        prefs.edit()
            .remove(KEY_MERCHANT_REGISTERED + walletAddress)
            .remove(KEY_CACHE_TIMESTAMP + walletAddress)
            .apply()
    }

    fun clearAllCache() {
        prefs.edit().clear().apply()
    }

    fun getCachedWalletAddresses(): List<String> {
        val allKeys = prefs.all.keys
        return allKeys
            .filter { it.startsWith(KEY_MERCHANT_REGISTERED) }
            .map { it.removePrefix(KEY_MERCHANT_REGISTERED) }
    }
} 
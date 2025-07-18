package com.focx.core.network

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network preferences manager for persisting network configuration
 */
@Singleton
class NetworkPreferences @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "network_preferences"
        private const val KEY_SELECTED_NETWORK = "selected_network"
        private const val KEY_CUSTOM_RPC_URL = "custom_rpc_url"
        private const val KEY_CUSTOM_WS_URL = "custom_ws_url"
        private const val KEY_AUTO_SWITCH_ENABLED = "auto_switch_enabled"
        private const val KEY_LAST_NETWORK_CHECK = "last_network_check"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get selected network type
     */
    fun getSelectedNetwork(): String {
        return prefs.getString(KEY_SELECTED_NETWORK, NetworkConfig.DEFAULT_NETWORK) ?: NetworkConfig.DEFAULT_NETWORK
    }

    /**
     * Set selected network type
     */
    fun setSelectedNetwork(networkType: String) {
        prefs.edit().putString(KEY_SELECTED_NETWORK, networkType).apply()
    }

    /**
     * Get custom RPC URL if set
     */
    fun getCustomRpcUrl(): String? {
        return prefs.getString(KEY_CUSTOM_RPC_URL, null)
    }

    /**
     * Set custom RPC URL
     */
    fun setCustomRpcUrl(url: String?) {
        prefs.edit().putString(KEY_CUSTOM_RPC_URL, url).apply()
    }

    /**
     * Get custom WebSocket URL if set
     */
    fun getCustomWsUrl(): String? {
        return prefs.getString(KEY_CUSTOM_WS_URL, null)
    }

    /**
     * Set custom WebSocket URL
     */
    fun setCustomWsUrl(url: String?) {
        prefs.edit().putString(KEY_CUSTOM_WS_URL, url).apply()
    }

    /**
     * Check if auto network switching is enabled
     */
    fun isAutoSwitchEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SWITCH_ENABLED, false)
    }

    /**
     * Set auto network switching preference
     */
    fun setAutoSwitchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SWITCH_ENABLED, enabled).apply()
    }

    /**
     * Get last network connectivity check timestamp
     */
    fun getLastNetworkCheck(): Long {
        return prefs.getLong(KEY_LAST_NETWORK_CHECK, 0L)
    }

    /**
     * Set last network connectivity check timestamp
     */
    fun setLastNetworkCheck(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_NETWORK_CHECK, timestamp).apply()
    }

    /**
     * Clear all network preferences
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        prefs.edit().putString(KEY_SELECTED_NETWORK, NetworkConfig.DEFAULT_NETWORK).remove(KEY_CUSTOM_RPC_URL)
            .remove(KEY_CUSTOM_WS_URL).putBoolean(KEY_AUTO_SWITCH_ENABLED, false).remove(KEY_LAST_NETWORK_CHECK).apply()
    }

    /**
     * Get effective RPC URL (custom or default)
     */
    fun getEffectiveRpcUrl(): String {
        return getCustomRpcUrl() ?: NetworkConfig.getRpcUrl(getSelectedNetwork())
    }

}
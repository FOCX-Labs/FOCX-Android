package com.focx.core.network

import android.content.Context
import com.focx.data.networking.KtorHttpDriver
import com.solana.rpc.SolanaRpcClient
import org.sol4k.Connection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network manager for handling network configuration updates
 * and dependency injection resets
 */
@Singleton
class NetworkManager @Inject constructor(
    private val context: Context,
    private val networkPreferences: NetworkPreferences,
    private val networkConnectionManager: NetworkConnectionManager
) {

    /**
     * Update network configuration and reset dependencies
     */
    fun updateNetworkConfiguration(networkType: String, customUrl: String?) {
        // Update NetworkConfig
        NetworkConfig.setCurrentNetwork(networkType)
        
        // Update NetworkPreferences
        networkPreferences.setSelectedNetwork(networkType)
        if (customUrl != null && customUrl.isNotBlank()) {
            networkPreferences.setCustomRpcUrl(customUrl)
        } else {
            networkPreferences.setCustomRpcUrl(null)
        }
        
        // Refresh network connections
        networkConnectionManager.refreshConnections()
    }

    /**
     * Get current effective RPC URL
     */
    fun getCurrentRpcUrl(): String {
        return networkPreferences.getEffectiveRpcUrl()
    }

    /**
     * Create new SolanaRpcClient with current configuration
     */
    fun createSolanaRpcClient(ktorHttpDriver: KtorHttpDriver): SolanaRpcClient {
        return SolanaRpcClient(getCurrentRpcUrl(), ktorHttpDriver)
    }

    /**
     * Create new Sol4k Connection with current configuration
     */
    fun createSol4kConnection(): Connection {
        return Connection(getCurrentRpcUrl())
    }

    /**
     * Get current network type
     */
    fun getCurrentNetwork(): String {
        return networkPreferences.getSelectedNetwork()
    }

    /**
     * Get current network display name
     */
    fun getCurrentNetworkDisplayName(): String {
        return NetworkConfig.getNetworkDisplayName(getCurrentNetwork())
    }

    /**
     * Check if current network is production
     */
    fun isCurrentNetworkProduction(): Boolean {
        return NetworkConfig.isProductionNetwork(getCurrentNetwork())
    }

    /**
     * Reset to default network configuration
     */
    fun resetToDefault() {
        networkPreferences.resetToDefaults()
        NetworkConfig.resetToDefault()
    }
}

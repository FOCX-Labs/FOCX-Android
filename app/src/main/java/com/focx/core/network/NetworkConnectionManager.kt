package com.focx.core.network

import com.focx.data.networking.KtorHttpDriver
import com.solana.rpc.SolanaRpcClient
import org.sol4k.Connection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connection manager for dynamic RPC client management
 * This class manages the creation and updating of network connections
 */
@Singleton
class NetworkConnectionManager @Inject constructor(
    private val networkPreferences: NetworkPreferences,
    private val ktorHttpDriver: KtorHttpDriver
) {

    @Volatile
    private var currentSolanaRpcClient: SolanaRpcClient? = null
    
    @Volatile
    private var currentSol4kConnection: Connection? = null
    
    @Volatile
    private var lastRpcUrl: String = ""

    /**
     * Get current SolanaRpcClient, creating if necessary
     */
    fun getSolanaRpcClient(): SolanaRpcClient {
        val currentUrl = networkPreferences.getEffectiveRpcUrl()
        
        // Check if we need to create a new client
        if (currentSolanaRpcClient == null || lastRpcUrl != currentUrl) {
            synchronized(this) {
                if (currentSolanaRpcClient == null || lastRpcUrl != currentUrl) {
                    currentSolanaRpcClient = SolanaRpcClient(currentUrl, ktorHttpDriver)
                    lastRpcUrl = currentUrl
                }
            }
        }
        
        return currentSolanaRpcClient!!
    }

    /**
     * Get current Sol4k Connection, creating if necessary
     */
    fun getSol4kConnection(): Connection {
        val currentUrl = networkPreferences.getEffectiveRpcUrl()
        
        // Check if we need to create a new connection
        if (currentSol4kConnection == null || lastRpcUrl != currentUrl) {
            synchronized(this) {
                if (currentSol4kConnection == null || lastRpcUrl != currentUrl) {
                    currentSol4kConnection = Connection(currentUrl)
                    lastRpcUrl = currentUrl
                }
            }
        }
        
        return currentSol4kConnection!!
    }

    /**
     * Force refresh connections (useful when network config changes)
     */
    fun refreshConnections() {
        synchronized(this) {
            val currentUrl = networkPreferences.getEffectiveRpcUrl()
            currentSolanaRpcClient = SolanaRpcClient(currentUrl, ktorHttpDriver)
            currentSol4kConnection = Connection(currentUrl)
            lastRpcUrl = currentUrl
        }
    }

    /**
     * Get current RPC URL
     */
    fun getCurrentRpcUrl(): String {
        return networkPreferences.getEffectiveRpcUrl()
    }

    /**
     * Check if connections need to be updated
     */
    fun needsUpdate(): Boolean {
        val currentUrl = networkPreferences.getEffectiveRpcUrl()
        return lastRpcUrl != currentUrl
    }
}

package com.focx.core.network

/**
 * Network configuration manager for Solana blockchain connections
 */
object NetworkConfig {

    // Network Types
    const val MAINNET = "mainnet"
    const val DEVNET = "devnet"
    const val TESTNET = "testnet"
    const val LOCALNET = "localnet"

    // Default Network (using testnet for development)
    const val DEFAULT_NETWORK = DEVNET

    // RPC Endpoints
    private const val SOLANA_MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"

    //  private   const val SOLANA_DEVNET_RPC_URL = "https://api.devnet.solana.com"
    private const val SOLANA_DEVNET_RPC_URL = "https://api.devnet.solana.com"
//        "https://rpc.ankr.com/solana_devnet/f317351e099431c4ac1057fa591c6835f7d13e305b4a7f0ceaf7868555cb20e4"

    //   private  const val SOLANA_DEVNET_RPC_URL =
//        "https://nameless-empty-general.solana-devnet.quiknode.pro/c58c56b52dd7081f9b7a5c48eeaefe1e097aaa48"
    private const val SOLANA_TESTNET_RPC_URL = "https://api.testnet.solana.com"
    private const val SOLANA_LOCALNET_RPC_URL = "http://127.0.0.1:8899"

    // Connection Settings
    const val CONNECTION_TIMEOUT_MS = 30000L
    const val READ_TIMEOUT_MS = 30000L
    const val WRITE_TIMEOUT_MS = 30000L
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 1000L

    // Rate Limiting
    const val MAX_REQUESTS_PER_SECOND = 10
    const val BURST_CAPACITY = 20

    /**
     * Public endpoint configuration data class
     */
    data class PublicEndpoint(
        val name: String,
        val url: String,
        val networkType: String,
        val isActive: Boolean = true,
        val priority: Int = 0
    )

    /**
     * List of 13 public endpoints from the configuration
     */
    val PUBLIC_ENDPOINTS = listOf(
        // Mainnet endpoints (10)
        PublicEndpoint(
            name = "Solana Foundation",
            url = "https://api.mainnet-beta.solana.com",
            networkType = MAINNET,
            priority = 1
        ),
        PublicEndpoint(
            name = "PublicNode",
            url = "https://solana-rpc.publicnode.com",
            networkType = MAINNET,
            priority = 2
        ),
        PublicEndpoint(
            name = "BlockEden",
            url = "https://api.blockeden.xyz/solana/KeCh6p22EX5AeRHxMSmc",
            networkType = MAINNET,
            priority = 3
        ),
        PublicEndpoint(
            name = "DRPC",
            url = "https://solana.drpc.org/",
            networkType = MAINNET,
            priority = 4
        ),
        PublicEndpoint(
            name = "Grove City",
            url = "https://solana.rpc.grove.city/v1/01fdb492",
            networkType = MAINNET,
            priority = 5
        ),
        PublicEndpoint(
            name = "LeoRPC",
            url = "https://solana.leorpc.com/?api_key=FREE",
            networkType = MAINNET,
            priority = 6
        ),
        PublicEndpoint(
            name = "OnFinality",
            url = "https://solana.api.onfinality.io/public",
            networkType = MAINNET,
            priority = 7
        ),
        PublicEndpoint(
            name = "GetBlock",
            url = "https://go.getblock.us/86aac42ad4484f3c813079afc201451c",
            networkType = MAINNET,
            priority = 8
        ),
        PublicEndpoint(
            name = "SolanaVibeStation",
            url = "https://public.rpc.solanavibestation.com/",
            networkType = MAINNET,
            priority = 9
        ),
        PublicEndpoint(
            name = "TheRPC",
            url = "https://solana.therpc.io",
            networkType = MAINNET,
            priority = 10
        ),
        
        // Devnet endpoints (2)
        PublicEndpoint(
            name = "Solana Foundation",
            url = "https://api.devnet.solana.com",
            networkType = DEVNET,
            priority = 1
        ),
        PublicEndpoint(
            name = "OnFinality",
            url = "https://solana-devnet.api.onfinality.io/public",
            networkType = DEVNET,
            priority = 2
        ),
        
        // Testnet endpoints (1)
        PublicEndpoint(
            name = "Solana Foundation",
            url = "https://api.testnet.solana.com",
            networkType = TESTNET,
            priority = 1
        )
    )

    /**
     * Get public endpoints for specified network type
     */
    fun getPublicEndpoints(networkType: String = currentNetwork): List<PublicEndpoint> {
        return PUBLIC_ENDPOINTS.filter { it.networkType == networkType && it.isActive }
            .sortedBy { it.priority }
    }

    /**
     * Get all active public endpoints
     */
    fun getAllActivePublicEndpoints(): List<PublicEndpoint> {
        return PUBLIC_ENDPOINTS.filter { it.isActive }.sortedBy { it.priority }
    }

    /**
     * Get endpoint by name
     */
    fun getEndpointByName(name: String): PublicEndpoint? {
        return PUBLIC_ENDPOINTS.find { it.name == name && it.isActive }
    }

    /**
     * Get endpoint by URL
     */
    fun getEndpointByUrl(url: String): PublicEndpoint? {
        return PUBLIC_ENDPOINTS.find { it.url == url && it.isActive }
    }

    /**
     * Get random endpoint for specified network type
     */
    fun getRandomEndpoint(networkType: String = currentNetwork): PublicEndpoint? {
        val endpoints = getPublicEndpoints(networkType)
        return if (endpoints.isNotEmpty()) {
            endpoints.random()
        } else null
    }

    /**
     * Get primary endpoint for specified network type (highest priority)
     */
    fun getPrimaryEndpoint(networkType: String = currentNetwork): PublicEndpoint? {
        return getPublicEndpoints(networkType).firstOrNull()
    }

    /**
     * Current active network type
     * Can be changed at runtime for testing purposes
     */
    @Volatile
    private var currentNetwork: String = DEFAULT_NETWORK

    /**
     * Get current network type
     */
    fun getCurrentNetwork(): String = currentNetwork

    /**
     * Set current network type
     * @param networkType The network type to switch to
     */
    fun setCurrentNetwork(networkType: String) {
        if (isValidNetwork(networkType)) {
            currentNetwork = networkType
        } else {
            throw IllegalArgumentException("Invalid network type: $networkType")
        }
    }

    /**
     * Get RPC URL for specified network type
     * Now uses public endpoints configuration
     */
    fun getRpcUrl(networkType: String = DEFAULT_NETWORK): String {
        // First try to get from public endpoints
        val publicEndpoint = getPrimaryEndpoint(networkType)
        if (publicEndpoint != null) {
            return publicEndpoint.url
        }
        
        // Fallback to original hardcoded URLs
        return when (networkType) {
            MAINNET -> SOLANA_MAINNET_RPC_URL
            DEVNET -> SOLANA_DEVNET_RPC_URL
            TESTNET -> SOLANA_TESTNET_RPC_URL
            LOCALNET -> SOLANA_LOCALNET_RPC_URL
            else -> SOLANA_TESTNET_RPC_URL
        }
    }

    /**
     * Check if network is production environment
     */
    fun isProductionNetwork(networkType: String): Boolean {
        return networkType == MAINNET
    }

    /**
     * Get network display name
     */
    fun getNetworkDisplayName(networkType: String): String {
        return when (networkType) {
            MAINNET -> "Mainnet Beta"
            DEVNET -> "Devnet"
            TESTNET -> "Testnet"
            LOCALNET -> "Localnet"
            else -> "Unknown Network"
        }
    }

    /**
     * Get current RPC URL based on active network
     */
    fun getCurrentRpcUrl(): String {
        return getRpcUrl(currentNetwork)
    }

    /**
     * Get current RPC URL with custom URL support
     * This method should be used when NetworkPreferences is available
     */
    fun getCurrentRpcUrlWithCustom(customUrl: String? = null): String {
        return customUrl ?: getRpcUrl(currentNetwork)
    }

    /**
     * Check if current network is production
     */
    fun isCurrentNetworkProduction(): Boolean {
        return isProductionNetwork(currentNetwork)
    }

    /**
     * Get current network display name
     */
    fun getCurrentNetworkDisplayName(): String {
        return getNetworkDisplayName(currentNetwork)
    }

    /**
     * Validate if network type is supported
     */
    private fun isValidNetwork(networkType: String): Boolean {
        return networkType in listOf(
            MAINNET,
            DEVNET,
            TESTNET,
            LOCALNET
        )
    }

    /**
     * Get all available networks
     */
    fun getAvailableNetworks(): List<String> {
        return listOf(
            MAINNET,
            DEVNET,
            TESTNET,
            LOCALNET
        )
    }

    /**
     * Network configuration data class
     */
    data class NetworkInfo(
        val type: String,
        val displayName: String,
        val rpcUrl: String,
        val wsUrl: String,
        val isProduction: Boolean
    )

    /**
     * Get network information for specified network type
     */
    fun getNetworkInfo(networkType: String = currentNetwork): NetworkInfo {
        return NetworkInfo(
            type = networkType,
            displayName = getNetworkDisplayName(networkType),
            rpcUrl = getRpcUrl(networkType),
            wsUrl = "wss:1.1.1.1",
            isProduction = isProductionNetwork(networkType)
        )
    }

    /**
     * Get all network information
     */
    fun getAllNetworkInfo(): List<NetworkInfo> {
        return getAvailableNetworks().map { getNetworkInfo(it) }
    }

    /**
     * Reset to default network
     */
    fun resetToDefault() {
        currentNetwork = DEFAULT_NETWORK
    }

    /**
     * Enable/disable endpoint by name
     */
    fun setEndpointActive(name: String, isActive: Boolean) {
        val endpoint = PUBLIC_ENDPOINTS.find { it.name == name }
        if (endpoint != null) {
            // Since PUBLIC_ENDPOINTS is immutable, we would need to recreate the list
            // For now, this is a placeholder for future implementation
            // In a real implementation, you might want to use a mutable list or database
        }
    }

    /**
     * Get endpoint statistics
     */
    fun getEndpointStatistics(): Map<String, Int> {
        return PUBLIC_ENDPOINTS.groupBy { it.networkType }
            .mapValues { it.value.size }
    }

    /**
     * Get endpoints by priority range
     */
    fun getEndpointsByPriorityRange(
        networkType: String = currentNetwork,
        minPriority: Int = 1,
        maxPriority: Int = Int.MAX_VALUE
    ): List<PublicEndpoint> {
        return getPublicEndpoints(networkType)
            .filter { it.priority in minPriority..maxPriority }
    }

    /**
     * Get endpoint count by network type
     */
    fun getEndpointCount(networkType: String): Int {
        return getPublicEndpoints(networkType).size
    }
}
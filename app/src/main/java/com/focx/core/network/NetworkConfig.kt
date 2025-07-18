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
    private const val SOLANA_DEVNET_RPC_URL =
        "https://rpc.ankr.com/solana_devnet/f317351e099431c4ac1057fa591c6835f7d13e305b4a7f0ceaf7868555cb20e4"

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
     */
    fun getRpcUrl(networkType: String = DEFAULT_NETWORK): String {
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
}
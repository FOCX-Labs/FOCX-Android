package com.focx.utils

import com.focx.core.constants.AppConstants
import com.focx.domain.entity.UnstakeRequest
import com.focx.domain.entity.Vault
import com.focx.domain.entity.VaultDepositor
import com.funkatronics.kborsh.Borsh
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.AccountInfoWithPublicKey
import com.solana.rpc.ProgramAccountsRequest
import com.solana.rpc.SolanaRpcClient
import java.math.BigInteger
import kotlin.math.pow

object VaultUtils {

    const val TAG = "VaultUtils"
    const val VAULT_NAME = "Insurance Fund Vault"

    suspend fun getVaultPda(): SolanaPublicKey {
        val vaultNameBuffer = ByteArray(32)
        val vaultNameBytes = VAULT_NAME.toByteArray()

        System.arraycopy(vaultNameBytes, 0, vaultNameBuffer, 0, minOf(vaultNameBytes.size, 32))

        return ProgramDerivedAddress.find(
            listOf(
                "vault".toByteArray(),
                vaultNameBuffer
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }

    suspend fun getVaultDepositorPda(account: SolanaPublicKey): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "vault_depositor".toByteArray(),
                getVaultPda().bytes,
                account.bytes
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }

    suspend fun getVaultTokenAccountPda(): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "vault_token_account".toByteArray(),
                getVaultPda().bytes
            ),
            AppConstants.App.getVaultProgramId()
        ).getOrNull()!!
    }

    /**
     * Calculate Annual Percentage Yield (APY) using compound interest
     * @param vault Vault entity containing total assets, total rewards, and creation time
     * @return APY percentage, returns 0.0 if insufficient data
     */
    fun calculateApy(vault: Vault): Double {
        if (vault.totalAssets == 0UL || vault.totalRewards == 0UL) {
            return 0.0
        }

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val vaultAgeDays = (currentTimeSeconds - vault.createdAt).toDouble() / (24 * 60 * 60)

        // Return 0 if vault age is less than 1 day to avoid extreme values
        if (vaultAgeDays < 1.0) {
            return 0.0
        }

        // Use current total assets as approximation of average assets
        val avgAssets = vault.totalAssets.toDouble()
        val totalYieldRate = vault.totalRewards.toDouble() / avgAssets
        val dailyYield = totalYieldRate / vaultAgeDays

        // APY (compound interest): (1 + daily yield)^365 - 1
        val apy = (1 + dailyYield).pow(365.0) - 1.0

        return apy * 100 // Convert to percentage
    }

    /**
     * Calculate Annual Percentage Rate (APR) using simple interest
     * @param vault Vault entity containing total assets, total rewards, and creation time
     * @return APR percentage, returns 0.0 if insufficient data
     */
    fun calculateApr(vault: Vault): Double {
        if (vault.totalAssets == 0UL || vault.totalRewards == 0UL) {
            return 0.0
        }

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val vaultAgeDays = (currentTimeSeconds - vault.createdAt).toDouble() / (24 * 60 * 60)

        // Return 0 if vault age is less than 1 day to avoid extreme values
        if (vaultAgeDays < 1.0) {
            return 0.0
        }

        // Use current total assets as approximation of average assets
        val avgAssets = vault.totalAssets.toDouble()
        val totalYieldRate = vault.totalRewards.toDouble() / avgAssets
        val dailyYield = totalYieldRate / vaultAgeDays

        // APR (simple interest): daily yield × 365
        val apr = dailyYield * 365 * 100

        return apr
    }

    suspend fun getTotalStakers(solanaRpcClient: SolanaRpcClient): Int {
        val vaultPda = getVaultPda()
        // IDL discriminator: [87, 109, 182, 106, 87, 96, 63, 211]
        val discriminator = byteArrayOf(87, 109, 182.toByte(), 106, 87, 96, 63, 211.toByte())
        val base58String = com.funkatronics.encoders.Base58.encode(discriminator)
        
        val hexString = discriminator.joinToString("") { "%02x".format(it) }
        
        Log.d(TAG, "Filtering accounts with discriminator: [$hexString] (Base58: $base58String)")
        
        val data = solanaRpcClient.getProgramAccounts(
            AppConstants.App.getVaultProgramId(),
        ).result
        
        val activeDepositors = filterAndParseVaultDepositors(vaultPda,data)
        
        Log.d(TAG, "total program accounts: ${data?.size}, active depositors (shares > 0): ${activeDepositors.size}")
        return activeDepositors.size
    }

    /**
     * Filter and parse VaultDepositor accounts, return depositors with shares > 0
     * @param programAccounts Program account data
     * @return Filtered list of VaultDepositors
     */
    private fun filterAndParseVaultDepositors(vaultPda: SolanaPublicKey, programAccounts: List<AccountInfoWithPublicKey<ByteArray>>?): List<VaultDepositor> {
        if (programAccounts == null) {
            Log.d(TAG, "No program accounts to filter")
            return emptyList()
        }

        val activeDepositors = mutableListOf<VaultDepositor>()
        val discriminator = byteArrayOf(87, 109, 182.toByte(), 106, 87, 96, 63, 211.toByte())

        programAccounts.forEachIndexed { index, accountInfo ->
            try {
                val accountData = accountInfo.account.data
                val publicKey = accountInfo.publicKey

                if (accountData == null) {
                    Log.w(TAG, "Account[$index] data is null")
                    return@forEachIndexed
                }

                // Get first 8 bytes for logging
                val first8Bytes = if (accountData.size >= 8) {
                    accountData.take(8).joinToString("") { "%02x".format(it) }
                } else {
                    accountData.joinToString("") { "%02x".format(it) }
                }

                Log.d(TAG, "Processing account[$index] ${publicKey} - first 8 bytes: $first8Bytes, data size: ${accountData.size}")

                // Check if first 8 bytes match discriminator
                if (accountData.size >= 8) {
                    val accountDiscriminator = accountData.take(8).toByteArray()
                    if (accountDiscriminator.contentEquals(discriminator)) {
                        Log.d(TAG, "Found VaultDepositor account[$index] ${publicKey}")
                        
                        try {
                            // Parse VaultDepositor using Borsh
                            val vaultDepositor = Borsh.decodeFromByteArray(VaultDepositor.serializer(), accountData)
                            
                            // Filter depositors with shares > 0
                            if (vaultDepositor.shares > 0UL && vaultPda == vaultDepositor.vault) {
                                activeDepositors.add(vaultDepositor)
                                Log.d(TAG, "Active depositor found - pubkey: ${publicKey}, pda: ${vaultDepositor.vault}, shares: ${vaultDepositor.shares}, authority: ${vaultDepositor.authority.base58()}")
                            } else {
                                Log.d(TAG, "Depositor ${publicKey} has 0 shares - skipping")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse VaultDepositor from account[$index] ${publicKey}: ${e.message}")
                        }
                    } else {
                        Log.v(TAG, "Account[$index] ${publicKey} discriminator mismatch")
                    }
                } else {
                    Log.w(TAG, "Account[$index] ${publicKey} data too small (${accountData.size} bytes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing account[$index]: ${e.message}")
            }
        }

        Log.d(TAG, "Found ${activeDepositors.size} active vault depositors")
        return activeDepositors
    }

    /**
     * Calculate user asset value based on vault and depositor data
     * Uses the same logic as the contract - available_assets and active_shares
     * Uses BigInteger to avoid overflow and improve precision
     * @param vault Vault entity containing vault information
     * @param vaultDepositor VaultDepositor entity containing user's depositor information
     * @return User's asset value in USDC with 2 decimal places (e.g., "123.45")
     */
    fun getUserAssetValueString(vault: Vault?, vaultDepositor: VaultDepositor?): String {
        var userAssetValue = getUserAssetValue(vault, vaultDepositor)
        return formatUsdc(userAssetValue)
    }

    fun formatUsdc(amount: ULong): String{
        return String.format("%.2f", amount.toDouble() / 1e9)
    }

    fun getUserAssetValue(vault: Vault?, vaultDepositor: VaultDepositor?): ULong {
        if (vault == null || vaultDepositor == null) {
            return 0UL
        }

        val userShares = vaultDepositor.shares
        val totalShares = vault.totalShares
        val totalAssets = vault.totalAssets

        // Use same logic as contract - available_assets and active_shares
        val pendingUnstakeShares = vault.pendingUnstakeShares
        val reservedAssets = vault.reservedAssets
        val availableAssets = totalAssets - reservedAssets
        val activeShares = totalShares - pendingUnstakeShares

        var userAssetValue = 0UL
        if (activeShares > 0UL && userShares > 0UL) {
            // Use BigInteger to avoid overflow and improve precision
            val userSharesBig = BigInteger.valueOf(userShares.toLong())
            val availableAssetsBig = BigInteger.valueOf(availableAssets.toLong())
            val activeSharesBig = BigInteger.valueOf(activeShares.toLong())

            // Calculate: (userShares * availableAssets) / activeShares
            val numerator = userSharesBig.multiply(availableAssetsBig)
            val result = numerator.divide(activeSharesBig)

            // Convert back to ULong, ensuring it doesn't exceed ULong.MAX_VALUE
            userAssetValue = result.toLong().toULong()
        }
        return userAssetValue
    }

    fun getPendingStakeValue(unstakeRequest: UnstakeRequest?): ULong {
        if (unstakeRequest == null) {
            return 0UL
        }

        val PRECISION = 1e12
        val shares = unstakeRequest.shares.toDouble()
        
        // Combine low and high parts of u128 assetPerShareAtRequest
        val assetPerShareLow = BigInteger.valueOf(unstakeRequest.assetPerShareAtRequest.toLong())
        val assetPerShareHigh = BigInteger.valueOf(unstakeRequest.assetPerShareAtRequest2.toLong())
        val assetPerShare = (assetPerShareHigh.shiftLeft(64).add(assetPerShareLow)).toDouble()
        
        val unstakeUSDCAmount = (shares * assetPerShare) / PRECISION
        
        return unstakeUSDCAmount.toULong()
    }

    fun getTotalPosition(vault: Vault?, vaultDepositor: VaultDepositor?): String{
        val total = getPendingStakeValue(vaultDepositor?.unstakeRequest) + getUserAssetValue(vault, vaultDepositor)
        return formatUsdc(total)
    }

}
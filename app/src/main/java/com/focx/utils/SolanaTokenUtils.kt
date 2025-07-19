package com.focx.utils

import com.focx.core.constants.AppConstants
import com.solana.publickey.SolanaPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.Security

/**
 * Utility class for Solana token operations
 * Provides helper methods for Associated Token Account calculations
 */
object SolanaTokenUtils {

    /**
     * Calculate Associated Token Account address
     * Equivalent to @solana/spl-token getAssociatedTokenAddress
     * @param mint Token mint address
     * @param owner Token account owner
     * @param tokenProgramId SPL Token Program ID (default: TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA)
     * @return Associated Token Account address
     */
    fun getAssociatedTokenAddress(
        mint: SolanaPublicKey,
        owner: SolanaPublicKey,
        tokenProgramId: SolanaPublicKey = SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID)
    ): SolanaPublicKey {
        val associatedTokenProgramId = SolanaPublicKey.from(AppConstants.App.ASSOCIATED_TOKEN_PROGRAM_ID)

        val seeds = listOf(
            owner.bytes,
            tokenProgramId.bytes,
            mint.bytes
        )

        val (address, _) = findProgramAddress(seeds, associatedTokenProgramId.bytes)
        return address
    }

    /**
     * Get USDC Associated Token Account for a given owner
     * @param owner Token account owner
     * @return USDC Associated Token Account address
     */
    fun getUsdcAssociatedTokenAccount(owner: SolanaPublicKey): SolanaPublicKey {
        val usdcMint = SolanaPublicKey.from(AppConstants.App.USDC_MINT)
        return getAssociatedTokenAddress(mint = usdcMint, owner = owner)
    }

    /**
     * Find program address (PDA) similar to Anchor's findProgramAddressSync
     * @param seeds List of seed byte arrays
     * @param programId Program ID byte array
     * @return Pair of (PublicKey, bump seed)
     */
    fun findProgramAddress(seeds: List<ByteArray>, programId: ByteArray): Pair<SolanaPublicKey, Int> {
        Security.addProvider(BouncyCastleProvider())
        require(programId.size == 32) { "programId must be 32 bytes" }
        require(seeds.size <= 16) { "Too many seeds" }
        for (seed in seeds) require(seed.size <= 32) { "Each seed must be <= 32 bytes" }

        for (bump in 255 downTo 0) {
            val allSeeds = seeds + byteArrayOf(bump.toByte())
            val data = ByteArrayOutputStream().apply {
                allSeeds.forEach { write(it) }
                write(programId)
                write("ProgramDerivedAddress".toByteArray(Charsets.UTF_8))
            }.toByteArray()
            val hash = MessageDigest.getInstance("SHA-256").digest(data)
            if (!isOnCurve(hash)) {
                return Pair(SolanaPublicKey(hash), bump)
            }
        }
        return Pair(ProgramIds.SYSTEM_PROGRAM, 255)
    }

    /**
     * Check if the public key is on the Ed25519 curve
     * @param publicKey 32 bytes
     * @return true = on curve (has private key, cannot be used as PDA), false = off curve (can be used as PDA)
     */
    private fun isOnCurve(publicKey: ByteArray): Boolean {
        if (publicKey.size != 32) return false
        return try {
            val point = ByteArray(32)
            System.arraycopy(publicKey, 0, point, 0, 32)
            Ed25519.validatePublicKeyFull(point, 0)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Common token mint addresses for easy access
     */
    object TokenMints {
        val USDC = SolanaPublicKey.from(AppConstants.App.USDC_MINT)
        val SOL = SolanaPublicKey.from("So11111111111111111111111111111111111111112") // Wrapped SOL
    }

    /**
     * Common program IDs for easy access
     */
    object ProgramIds {
        val TOKEN_PROGRAM = SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID)
        val ASSOCIATED_TOKEN_PROGRAM = SolanaPublicKey.from(AppConstants.App.ASSOCIATED_TOKEN_PROGRAM_ID)
        val SYSTEM_PROGRAM = SolanaPublicKey.from("11111111111111111111111111111111")
    }
}
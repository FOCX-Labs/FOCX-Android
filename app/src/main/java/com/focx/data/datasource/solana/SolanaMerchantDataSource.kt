package com.focx.data.datasource.solana

import android.content.Context
import android.net.Uri
import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConfig
import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.AccountInfo
import com.solana.rpc.SolanaRpcClient
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaMerchantDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) : IMerchantRepository {

    companion object {
        private const val TAG = "SMDS"
    }

    override suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(TAG, "Starting merchant registration for: ${merchantRegistration.name}")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "registerMerchantAtomic authResult:${authResult}")
                val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)
                val programId = SolanaPublicKey.from(merchantRegistration.programId)

                // Calculate PDAs (Program Derived Addresses)
                val globalRootPda = findProgramAddress(
                    listOf("global_id_root".toByteArray()), programId.bytes
                )

                val merchantInfoPda = findProgramAddress(
                    listOf(
                        "merchant_info".toByteArray(), merchantPublicKey.bytes
                    ), programId.bytes
                )

                val merchantIdAccountPda = findProgramAddress(
                    listOf(
                        "merchant".toByteArray(), merchantPublicKey.bytes
                    ), programId.bytes
                )

                val initialChunkPda = findProgramAddress(
                    listOf(
                        "id_chunk".toByteArray(),
                        merchantPublicKey.bytes,
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
                    ), programId.bytes
                )

                Log.d(TAG, "Calculated PDAs:")
                Log.d(TAG, "  globalRootPda: ${globalRootPda.first.base58()}")
                Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
                Log.d(TAG, "  merchantIdAccountPda: ${merchantIdAccountPda.first.base58()}")
                Log.d(TAG, "  initialChunkPda: ${initialChunkPda.first.base58()}")

                // Create instruction data for registerMerchantAtomic
                val instructionData = createRegisterMerchantAtomicInstructionData(
                    merchantRegistration.name, merchantRegistration.description
                )

                // Create account metas for the instruction
                val accountMetas = listOf(
                    AccountMeta(merchantPublicKey, true, true), // merchant (signer, writable)
                    AccountMeta(merchantPublicKey, true, true), // payer (signer, writable)
                    AccountMeta(globalRootPda.first, false, true), // globalRoot (writable)
                    AccountMeta(merchantInfoPda.first, false, true), // merchantInfo (writable)
                    AccountMeta(merchantIdAccountPda.first, false, true), // merchantIdAccount (writable)
                    AccountMeta(initialChunkPda.first, false, true), // initialChunk (writable)
                    AccountMeta(
                        SolanaPublicKey.from("11111111111111111111111111111112"), false, false
                    ) // systemProgram
                )

                // Create the instruction
                val instruction = TransactionInstruction(
                    programId, accountMetas, instructionData
                )

                // Create and sign the transaction
                // Get the actual recent blockhash from Solana network with timeout handling
                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)
                Log.d(TAG, "  signAndSendTransactions: before}")
                val result = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "  signAndSendTransactions:${result}")
                result
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(
                            TAG, "Registration successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantRegistration.merchantPublicKey,
                            errorMessage = null
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Registration failed: ${result.e.message}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Transaction failed: ${result.e.message}"
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Registration exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    override suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration
    ): MerchantRegistrationResult {
        return MerchantRegistrationResult(
            success = false,
            transactionSignature = null,
            merchantAccount = null,
            errorMessage = "ActivityResultSender is required for wallet transactions. Use the overloaded method."
        )
    }

    override suspend fun getMerchantStatus(walletAddress: String): Flow<MerchantStatus> = flow {
        try {
            // TODO: Implement real Solana network query
            // This should query the merchant account data from the Solana blockchain
            throw RuntimeException("getMerchantStatus requires real Solana network implementation")
        } catch (e: Exception) {
            emit(
                MerchantStatus(
                    isRegistered = false,
                    merchantAccount = null,
                    registrationDate = null,
                    securityDeposit = null,
                    status = AppConstants.Merchant.DEFAULT_STATUS
                )
            )
        }
    }

    override suspend fun getMerchantAccountData(merchantAccount: String): Flow<MerchantStatus> = flow {
        try {
            Log.d(TAG, "Querying merchant account data for: $merchantAccount")

            val merchantPublicKey = SolanaPublicKey.from(merchantAccount)
            val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)

            // Calculate merchant info PDA
            val merchantInfoPda = findProgramAddress(
                listOf("merchant_info".toByteArray(), merchantPublicKey.bytes), programId.bytes
            )

            // Calculate merchant ID account PDA
            val merchantIdAccountPda = findProgramAddress(
                listOf("merchant".toByteArray(), merchantPublicKey.bytes), programId.bytes
            )

            Log.d(TAG, "Calculated PDAs:")
            Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
            Log.d(TAG, "  merchantIdAccountPda: ${merchantIdAccountPda.first.base58()}")

            // Query account info from Solana network using SolanaRpcClient
            val rpcUri = Uri.parse(NetworkConfig.getRpcUrl())

            // Check if merchant info account exists
            val merchantInfoAccountInfo = queryAccountInfo(rpcUri, merchantInfoPda.first.base58())
            val merchantIdAccountInfo = queryAccountInfo(rpcUri, merchantIdAccountPda.first.base58())

            if (merchantInfoAccountInfo != null && merchantIdAccountInfo != null) {
                // Parse account data to extract merchant information
                val merchantStatus = parseMerchantAccountData(
                    merchantInfoAccountInfo, merchantIdAccountInfo, merchantAccount
                )

                Log.d(TAG, "Merchant account found and parsed successfully")
                emit(merchantStatus)
            } else {
                Log.d(TAG, "Merchant account not found or not initialized")
                emit(
                    MerchantStatus(
                        isRegistered = false,
                        merchantAccount = null,
                        registrationDate = null,
                        securityDeposit = null,
                        status = AppConstants.Merchant.DEFAULT_STATUS
                    )
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error querying merchant account data: ${e.message}", e)
            emit(
                MerchantStatus(
                    isRegistered = false,
                    merchantAccount = null,
                    registrationDate = null,
                    securityDeposit = null,
                    status = AppConstants.Merchant.DEFAULT_STATUS
                )
            )
        }
    }

    /**
     * Find program address (PDA) similar to Anchor's findProgramAddressSync
     * Simplified implementation for development
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
        return Pair(SolanaPublicKey.from("11111111111111111111111111111112"), 255);
    }

    /**
     * Check if the public key is on the Ed25519 curve (i.e., is a valid public key)
     * @param publicKey 32 bytes
     * @return true = on curve (has private key, cannot be used as PDA), false = off curve (can be used as PDA)
     */
    fun isOnCurve(publicKey: ByteArray): Boolean {
        if (publicKey.size != 32) return false
        // Ed25519 检查
        return try {
            val point = ByteArray(32)
            System.arraycopy(publicKey, 0, point, 0, 32)
            Ed25519.validatePublicKeyFull(point, 0)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Query account info from Solana RPC using SolanaRpcClient
     */
    private suspend fun queryAccountInfo(rpcUri: Uri, accountAddress: String): AccountInfo<ByteArray>? {
        return try {
            val publicKey = SolanaPublicKey.from(accountAddress)
            val rpcResponse = solanaRpcClient.getAccountInfo(publicKey)
            rpcResponse.result
        } catch (e: Exception) {
            Log.e(TAG, "Error querying account info for $accountAddress: ${e.message}", e)
            null
        }
    }

    /**
     * Parse merchant account data from Solana account info
     */
    private fun parseMerchantAccountData(
        merchantInfoAccountInfo: AccountInfo<ByteArray>?,
        merchantIdAccountInfo: AccountInfo<ByteArray>?,
        merchantAccount: String
    ): MerchantStatus {
        try {
            // Extract account data (base64 encoded)
            if (merchantInfoAccountInfo != null && merchantIdAccountInfo != null) {
                // Parse the account data based on your programs data structure
                val merchantInfoData = merchantInfoAccountInfo.data
                val merchantIdData = merchantIdAccountInfo.data

                Log.d(TAG, "Merchant info data: ${merchantInfoData?.contentToString()}")
                Log.d(TAG, "Merchant ID data: ${merchantIdData?.contentToString()}")

                // For now, return a basic registered status
                // You should implement proper deserialization based on your Solana program's account structure
                return MerchantStatus(
                    isRegistered = true,
                    merchantAccount = merchantAccount,
                    registrationDate = System.currentTimeMillis().toString(), // You should parse this from account data
                    securityDeposit = merchantInfoAccountInfo.lamports?.toString(), // Use lamports as security deposit
                    status = "active" // You should parse this from account data
                )
            }

            return MerchantStatus(
                isRegistered = false,
                merchantAccount = null,
                registrationDate = null,
                securityDeposit = null,
                status = AppConstants.Merchant.DEFAULT_STATUS
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing merchant account data: ${e.message}", e)
            return MerchantStatus(
                isRegistered = false,
                merchantAccount = null,
                registrationDate = null,
                securityDeposit = null,
                status = AppConstants.Merchant.DEFAULT_STATUS
            )
        }
    }

    /**
     * Create instruction data for registerMerchantAtomic method
     * Based on the Anchor program method signature
     */
    private fun createRegisterMerchantAtomicInstructionData(
        merchantName: String, merchantDescription: String
    ): ByteArray {
        // Instruction discriminator for registerMerchantAtomic method
        // This should match the method hash in your Anchor program
        val discriminator = byteArrayOf(
            0x8C.toByte(),
            0x97.toByte(),
            0x25.toByte(),
            0x8F.toByte(),
            0x4C.toByte(),
            0x2A.toByte(),
            0x9D.toByte(),
            0x12.toByte()
        )

        // Serialize merchant name
        val nameBytes = merchantName.toByteArray(Charsets.UTF_8)
        val nameLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(nameBytes.size).array()

        // Serialize merchant description
        val descriptionBytes = merchantDescription.toByteArray(Charsets.UTF_8)
        val descriptionLength =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(descriptionBytes.size).array()

        // Combine all data
        return discriminator + nameLength + nameBytes + descriptionLength + descriptionBytes
    }
}
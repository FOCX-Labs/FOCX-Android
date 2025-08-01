package com.focx.data.datasource.solana

import android.content.Context
import com.focx.core.constants.AppConstants
import com.focx.core.constants.AppConstants.App.SPL_TOKEN_PROGRAM_ID
import com.focx.core.constants.AppConstants.Merchant.DEFAULT_STATUS
import com.focx.core.network.NetworkConfig
import com.focx.domain.entity.Merchant
import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.DebugUtils
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.ShopUtils.genTransactionInstruction
import com.focx.utils.ShopUtils.getAssociatedTokenAddress
import com.focx.utils.ShopUtils.getDepositEscrowPda
import com.focx.utils.ShopUtils.getGlobalRootPda
import com.focx.utils.ShopUtils.getInitialChunkPda
import com.focx.utils.ShopUtils.getMerchantIdPda
import com.focx.utils.ShopUtils.getMerchantInfoPda
import com.focx.utils.ShopUtils.getSystemConfigPDA
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RegisterMerchantAtomicArgs(
    val name: String,
    val description: String
)

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
            Log.d(
                TAG,
                "Starting registerMerchantWithAnchor, walletAdapter authToken: ${walletAdapter.authToken}"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG, "registerMerchantWithAnchor authResult.authToken:${authResult.authToken}"
                )

                val registerInstruction =
                    genRegisterInstruction(merchantRegistration, activityResultSender)
                val depositInstruction =
                    genDepositMerchantDepositInstruction(
                        merchantRegistration.merchantPublicKey,
                        merchantRegistration.securityDeposit)

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder()
                    .addInstruction(registerInstruction)
                    .addInstruction(depositInstruction)
                    .setRecentBlockhash(recentBlockhash)
                    .build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Anchor): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (Anchor): $signResult")
                signResult
            }


            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(
                            TAG,
                            "Anchor registration successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantRegistration.merchantPublicKey
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false, errorMessage = "No signature returned from transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false, errorMessage = "No wallet found"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Anchor registration failed: ${result.e}")
                    MerchantRegistrationResult(
                        success = false, errorMessage = "Transaction failed: ${result.e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Anchor registration exception:", e)
            MerchantRegistrationResult(
                success = false, errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    private suspend fun genRegisterInstruction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): TransactionInstruction {
        Log.d(TAG, "genRegisterTransaction start")

        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)


        val globalRootPda = getGlobalRootPda()
        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey)
        val systemConfigPda = getSystemConfigPDA()
        val merchantIdAccountPda = getMerchantIdPda(merchantPublicKey)
        val initialChunkPda = getInitialChunkPda(merchantPublicKey)

        val args = RegisterMerchantAtomicArgs(
            name = merchantRegistration.name, description = merchantRegistration.description
        )

        val instructionData = Borsh.encodeToByteArray(AnchorInstructionSerializer("register_merchant_atomic"), args)

        val accountMetas = listOf(
            AccountMeta(
                merchantPublicKey, true, true
            ), AccountMeta(merchantPublicKey, true, true), AccountMeta(
                globalRootPda.getOrNull()!!, false, true
            ), AccountMeta(
                merchantInfoPda.getOrNull()!!, false, true
            ), AccountMeta(
                systemConfigPda.getOrNull()!!, false, false
            ), AccountMeta(
                merchantIdAccountPda.getOrNull()!!, false, true
            ), AccountMeta(
                initialChunkPda.getOrNull()!!, false, true
            ), AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )


        return genTransactionInstruction( accountMetas, instructionData)
    }

    private suspend fun genDepositMerchantDepositInstruction(
        merchantAccount: String,
        depositAmount: ULong
    ): TransactionInstruction {
        Log.d(TAG, "genDepositMerchantDepositTransaction start")

        val merchantPublicKey = SolanaPublicKey.from(merchantAccount)


        val instructionData = Borsh.encodeToByteArray(
            AnchorInstructionSerializer("manage_deposit"),
            depositAmount)

        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey)
        val systemConfigPda = getSystemConfigPDA()
        val depositEscrowPda = getDepositEscrowPda()

        val merchantTokenAccount = getAssociatedTokenAddress( merchantPublicKey)

        val accountMetas = listOf(
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(merchantInfoPda.getOrNull()!!, false, true),
            AccountMeta(systemConfigPda.getOrNull()!!, false, false),
            AccountMeta(merchantTokenAccount.getOrNull()!!, false, true),
            AccountMeta(AppConstants.App.getMint(), false, false),
            AccountMeta(depositEscrowPda.getOrNull()!!, false, true),
            AccountMeta(SolanaPublicKey.from(SPL_TOKEN_PROGRAM_ID), false, false),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )

        return genTransactionInstruction(accountMetas, instructionData)
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

    override suspend fun getMerchantAccountData(walletAddress: String): Flow<MerchantStatus> =
        flow {
            try {
                val merchantPublicKey = SolanaPublicKey.from(walletAddress)
                val pda = getMerchantInfoPda(SolanaPublicKey.from(walletAddress))
                DebugUtils.decodeCreateProductBase("9923fb426dc61a940d0000006950686f6e652031352050726f2b000000e69c80e696b0e6acbee88bb9e69e9ce6898be69cbaefbc8ce9858de5a4874131372050726fe88aafe7898700743ba40b0000000300000006000000e6898be69cba06000000e88bb9e69e9c060000006950686f6e656400000000000000ba09cc80988c16bdfbd50abbbc101d2459c2cf3d1e48bf25e922682da2a0f9fe12000000e9bb98e8aea4e58f91e8b4a7e59cb0e782b9")
                DebugUtils.decodeCreateProductBase("9923fb426dc61a9406000000e69bb4e5889a09000000e5889ae8bf9be5aeb600362f5f3b000000010000000e0000004469676974616c2043616d6572619a02000000000000ba09cc80988c16bdfbd50abbbc101d2459c2cf3d1e48bf25e922682da2a0f9fe1900000044656661756c74205368697070696e67204c6f636174696f6e")
                DebugUtils.decodeCreateProductExtended("0e257c9b6d1ac91aa068060000000000020000001e00000068747470733a2f2f6578616d706c652e636f6d2f696d616765312e6a70671e00000068747470733a2f2f6578616d706c652e636f6d2f696d616765322e6a7067020000000c000000e4b8ade59bbde5a4a7e9998609000000e6b8afe6beb3e58fb0030000000c000000e9a1bae4b8b0e5bfabe980920c000000e4baace4b89ce789a9e6b5810c000000e59c86e9809ae9809fe98092")
                val pbPda = ShopUtils.getProductBasePDA(420000UL)
                Log.d("DebugUtils", "pbpda 420000UL :$pbPda")

                ShopUtils.getMerchantProducts(SolanaPublicKey.from(walletAddress), solanaRpcClient)

                Log.d(TAG, "merchant pad: ${pda.getOrNull()!!.base58()}")
                val merchant = solanaRpcClient.getAccountInfo<Merchant>(pda.getOrNull()!!).result
                val orderCount = ShopUtils.getMerchantOrderCount(ShopUtils.getMerchantOrderCountPDA(merchantPublicKey).getOrNull()!!, solanaRpcClient)

                if (!(merchant == null || merchant.data == null)) {
                    Log.d(TAG, "merchant info: ${merchant.data!!.name}, ${merchant.data!!.description}, ${merchant.data!!.depositAmount},")

                    val merchantStatus = MerchantStatus(
                        isRegistered = true,
                        merchantAccount = walletAddress,
                        orderCounts = orderCount,
                        productCounts = merchant.data!!.productCount,
                        registrationDate = merchant.data!!.createdAt.toString(),
                        securityDeposit = merchant.data!!.depositAmount,
                        status = DEFAULT_STATUS
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
                            status = DEFAULT_STATUS
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying merchant account data: ${e.message}", e)
//                if (AbortFlowException.)
//                emit(
//                    MerchantStatus(
//                        isRegistered = false,
//                        merchantAccount = null,
//                        registrationDate = null,
//                        securityDeposit = null,
//                        status = AppConstants.Merchant.DEFAULT_STATUS
//                    )
//                )
            }
        }

    override suspend fun depositMerchantFunds(
        merchantAccount: String, depositAmount: ULong, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG,
                "Starting depositMerchantFunds for merchant: $merchantAccount, amount: $depositAmount"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val depositInstruction =
                    genDepositMerchantDepositInstruction(
                        merchantAccount,
                        depositAmount)

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder()
                    .addInstruction(depositInstruction)
                    .setRecentBlockhash(recentBlockhash)
                    .build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Anchor): before")
                val result = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (Anchor): $result")
                result
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(TAG, "Deposit successful: ${Base58.encodeToString(signature)}")
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantAccount,
                            errorMessage = null
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from deposit transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found for deposit"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Deposit failed: ${result.e.message}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Deposit transaction failed: ${result.e.message}"
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Deposit exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Deposit failed: ${e.message}"
            )
        }
    }
}